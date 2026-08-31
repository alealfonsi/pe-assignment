# AI Design: LLM choice, calls, and agent instructions

This document covers the assignment "Extras" — the LLM of choice and the agent instructions —
and explains **every call made to the LLM: what it does, what goes in, what comes out, and why
it exists**.

## LLM of choice

**Claude Opus 5 (`claude-opus-5`) via the official Anthropic Java SDK (`com.anthropic:anthropic-java`).**

Why this choice:

- **Structured outputs.** The SDK derives a JSON schema from a Java record and the API
  constrains generation to it. Both pipeline calls get compile-time-typed results
  (`TriagePlan`, `RiskAnalysisResult`) with no hand-written JSON parsing and no
  "please answer in JSON" prompt fragility.
- **Strong analytical writing** for a compliance-adjacent domain where phrasing matters
  (grounded findings, no speculation about intent, tipping-off awareness).
- **Adaptive thinking on by default** on Opus 5 — the model reasons before answering the
  risk-grading call with no extra configuration.
- **First-party Java SDK** fits the Spring Boot stack: typed builders, typed exceptions,
  timeouts and retries out of the box.

The assignment allows stubbing the LLM, so the provider sits behind an interface
(`LlmClient`) with two implementations selected at startup (`app.ai.mode`):

| Mode | Backend | When |
|---|---|---|
| `AUTO` (default) | Anthropic if `ANTHROPIC_API_KEY` is set, else stub | demo-friendly default |
| `ANTHROPIC` | real Claude; startup fails without a key | production posture |
| `STUB` | deterministic offline engine | tests, offline demos |

The stub is not a mock that returns canned text: it consumes the same activity digest,
produces the same output types, follows the same policy score bands, and drives the same
retrieval and persistence — so the *entire* pipeline is real in both modes; only the
"reasoning engine" differs.

## Pipeline overview

```
operator clicks "Run AI analysis"
        │
        ▼
(0) ActivityDigestBuilder  ── deterministic Java, no LLM
        │  compact digest: totals, corridors, MCCs, fired rules, notable tx
        ▼
(1) LLM CALL 1 "triage & retrieval planning"  → {salientSignals[], retrievalQueries[]}
        │
        ▼
(2) BM25 retrieval over policy_chunks (top-6)  ── no LLM
        │  policy excerpts [S1]..[S6]
        ▼
(3) LLM CALL 2 "risk analysis"  → {riskLevel, summary, findings[], recommendations[]}
        │
        ▼
(4) semantic validation ──(invalid)──► LLM CALL 3 "repair" (same as call 2 + feedback; at most once)
        │
        ▼
(5) persist to ai_analyses (result + retrieval context + usage + latency)
```

## Step 0 — Activity digest (deliberately *not* an LLM call)

`ActivityDigestBuilder` compacts the customer's activity into a small structured text block:
per-type counts and completed totals by currency, payment corridors (`SWIFT->AE: 2`), card
MCC histogram, crypto counterparties, every fired risk rule with its logic and score, and a
"notable transactions" list (every rule-flagged transaction plus the largest unflagged ones
for context).

Why deterministic code instead of handing the LLM raw rows:

- **Bounded prompt size** regardless of how much history a customer has.
- **Traceability**: every fact the model can cite exists in the digest, and the digest is
  reproducible from the DB.
- **No leakage of irrelevant PII**: the digest carries only what the analysis needs.

## LLM call 1 — Triage & retrieval planning

- **Input**: triage system prompt + the rendered digest.
- **Output** (schema-constrained): `TriagePlan { salientSignals: string[], retrievalQueries: string[] }`
- **Config**: `max_tokens=2048`; model default thinking; no sampling overrides.

**What it does**: reads the digest, names the risk signals that actually matter, and writes
2–6 short keyword queries against the internal policy knowledge base (it is told which topic
areas the KB covers, but sees no policy text yet).

**Why it exists** (instead of retrieving with the raw digest or hard-coding queries):

- The mapping "signals in data → policies worth reading" is judgment, and it is exactly what
  an analyst does first. Example from the seeded data: three 9.9k wires within 26 hours →
  query *structuring sub-threshold transfers*, which retrieves the structuring policy even
  though no transaction contains the word "structuring". Lexical search on the raw digest
  could never make that leap.
- It keeps retrieval **adaptive**: new rule types or unusual combinations don't require code
  changes for the right policies to be found.
- The triage output is also **shown to the operator** ("Triage signals & retrieval queries"
  panel) and persisted, making the pipeline inspectable: you can see what the model thought
  was salient *before* it read any policy.
- It always adds a query about risk-classification criteria, so call 2 can grade against the
  bank's own definitions rather than the model's general intuition.

## Step 2 — Retrieval (no LLM)

BM25 (Okapi, in-memory, ~80 lines of code) over the policy corpus: 7 markdown documents
chunked by `##` section into ~33 chunks, loaded idempotently at startup. Each triage query
runs independently; results are merged per-chunk by best score; global top-6 with document
and section titles go into the next prompt (and into the persisted record, and into the UI's
"Policy sources used" panel).

Why lexical rather than embeddings: the corpus is tiny and terminology-dense, and the queries
are written by the LLM in the same vocabulary — BM25 is precise here, fully deterministic,
and keeps the app runnable with zero external services (Anthropic has no embeddings API, so
vectors would force a second provider or a local model). `PolicyRetriever` is the seam where
a vector store could replace it without touching the pipeline.

## LLM call 2 — Risk analysis (the main call)

- **Input**: analysis system prompt + the digest + the numbered policy excerpts `[S1]..[S6]`.
- **Output** (schema-constrained):

```json
{
  "riskLevel": "LOW | MEDIUM | HIGH | CRITICAL",
  "summary": "3-6 sentence executive summary naming the principal driver",
  "findings": [
    { "title": "...", "severity": "LOW|MEDIUM|HIGH|CRITICAL",
      "evidence": "specific transactions/amounts/dates from the digest",
      "policyRefs": ["Crypto-Asset Activity Policy - Mixing and tumbling services"] }
  ],
  "recommendations": ["ordered operator actions"]
}
```

- **Config**: `max_tokens=8192` (configurable); refusal and truncation stop-reasons are
  detected and surfaced as clean errors.

**What it does**: produces the three things the assignment asks of the AI analysis — a risk
level, a summary of findings, and recommendations — as one grounded, policy-based judgment.

**Why it is one call** (not one per activity type, not level-then-explain): the risk grade
must weigh *combinations* across signal families — the classification policy itself says
"combined independent signal families outweigh repetition of one family" — so splitting the
reasoning would discard exactly the cross-signal context that determines the grade. One call
also means the level, findings and recommendations are guaranteed mutually consistent.

**How it is grounded**: the system prompt forbids inventing transactions or policy rules,
requires findings to cite digest facts as evidence and to reference the supplied excerpts by
document + section title in `policyRefs`, requires the risk level to be graded per the
retrieved classification criteria, and requires recommendations to be actions an operator can
take under the retrieved escalation policy (including never recommending the customer be told
of suspicion — the tipping-off prohibition, which the policy corpus states and the prompt
reinforces).

## LLM call 3 — Repair (conditional, at most once)

Schema conformance is enforced by structured outputs, but semantic quality is validated in
Java: risk level present, non-empty summary, every finding carrying a title/severity/evidence,
at least one recommendation. If validation fails, the analysis call is re-issued once with
the user prompt suffixed by the concrete validation feedback ("your previous answer was
rejected: recommendations must contain at least one actionable item…"). A second failure is a
hard error (HTTP 502 with a clear message), never a silently degraded analysis.

**Why**: defense in depth at negligible cost — the call happens only when needed, and the
persisted record therefore always satisfies the output contract that the UI and reviewers
rely on.

## What is persisted (assignment requirement 5)

Every run stores in `ai_analyses`: risk level, summary, findings JSON, recommendations JSON,
**salient signals and retrieval queries from call 1**, **the policy excerpts used** (document,
section, excerpt text), LLM mode and model name, input/output token counts, latency, the
operator who ran it and the timestamp. History is browsable per customer in the UI; records
are never edited (per the record-keeping policy in the corpus — corrections are new runs).

## Agent instructions (verbatim summary)

The two system prompts live in `PromptBuilder.java`; they are reproduced here as the
assignment's "summary of agent instructions".

**Call 1 — triage system prompt (condensed):**

> You are a financial-crime triage assistant inside a customer activity analytics tool used by
> bank customer-care operators. You will receive a compact digest of one customer's recent
> activity (card, payment, cryptocurrency), including which automated risk rules fired.
> 1) `salientSignals`: identify the risk-relevant signals — short factual phrases grounded ONLY
> in the digest; never invent transactions, amounts or counterparties.
> 2) `retrievalQueries`: 2–6 short keyword-style queries for the internal policy knowledge base
> (topics: AML monitoring, high-risk jurisdictions, crypto policy, card fraud, structuring,
> escalation/SAR, risk classification) such that the retrieved documents let an analyst
> evaluate every signal. Always include one query about risk-level classification criteria.
> If no risk signals exist, say so and still query baseline classification/monitoring policy.

**Call 2 — analysis system prompt (condensed):**

> You are a senior financial-crime analyst producing a risk analysis for a customer-care
> operator. You receive an ACTIVITY DIGEST and numbered POLICY EXCERPTS `[S1]…`.
> Produce riskLevel, summary, findings, recommendations. Rules: ground every statement in the
> digest and excerpts only — never invent transactions or policy rules; if evidence is
> insufficient, say so instead of speculating. Grade riskLevel using the classification
> criteria in the excerpts; combined independent signal families outweigh repetition of one
> family; failed and reversed transactions count as behaviour. Every finding must cite its
> evidence and list the policy excerpts it relies on (document + section title).
> Recommendations must be concrete operator actions consistent with the escalation paths in
> the excerpts, ordered by priority; never recommend telling the customer they are under
> suspicion (tipping-off). Write factually; describe patterns, not intent.

## The stub, precisely

`StubLlmClient` mirrors both calls deterministically:

- **Triage**: signals = the fired rules with counts/scores; queries = a fixed keyword map from
  rule names to KB queries, plus corridor-triggered and always-on grounding queries — the same
  shape call 1 produces.
- **Analysis**: risk level from the aggregate rule score using the bands written in the AML
  policy (0–19 → LOW, 20–49 → MEDIUM, 50–89 → HIGH, ≥90 → CRITICAL) with the mixer-exposure
  override from the crypto policy (→ CRITICAL); findings built per fired rule with evidence
  quoted from the digest's notable transactions; `policyRefs` chosen by keyword overlap with
  the *actually retrieved* chunks; recommendations mapped from the graded level to the
  escalation paths in the policy corpus.

So even offline, what you see in the UI — grades, citations, sources — reflects real
retrieval over the real corpus against the real data.

## Cost & latency notes

- Two LLM calls per analysis (three only on repair). The triage call is small
  (`max_tokens=2048`); the digest bounds the input side of both calls.
- Prompt structure keeps the stable system prompts first, per-request content last —
  cache-friendly if prompt caching is enabled later.
- Token usage per analysis is captured from the API response and persisted/displayed, so real
  cost is observable per run.

## Known limitations / next steps

- BM25 has no semantic recall for vocabulary the triage call doesn't produce; a vector store
  (e.g. pgvector) can be slotted in behind `PolicyRetriever`.
- Analyses run synchronously (seconds-scale with the real model); a job queue + polling would
  suit production UX better.
- Prompt caching (`cache_control` on the system prompts) and batch analysis of many customers
  are natural cost optimizations once volume justifies them.
