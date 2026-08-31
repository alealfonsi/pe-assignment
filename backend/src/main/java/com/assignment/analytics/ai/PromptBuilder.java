package com.assignment.analytics.ai;

import com.assignment.analytics.ai.ActivityDigest.NotableTransaction;
import com.assignment.analytics.ai.ActivityDigest.RuleFire;
import com.assignment.analytics.ai.ActivityDigest.TypeSummary;
import com.assignment.analytics.rag.RetrievedChunk;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds the prompts for both LLM calls. Prompts are assembled from the
 * deterministic {@link ActivityDigest} - raw DB rows never reach the LLM, and
 * every fact in the prompt is traceable back to the database.
 *
 * The system prompts double as the "agent instructions" documented in
 * docs/AI_DESIGN.md; keep the two in sync when editing.
 */
@Component
public class PromptBuilder {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    static final String TRIAGE_SYSTEM = """
            You are a financial-crime triage assistant inside a customer activity analytics tool \
            used by bank customer-care operators.

            You will receive a compact digest of one customer's recent activity (card, payment and \
            cryptocurrency transactions), including which automated risk rules fired.

            Your task has two parts:
            1. salientSignals: identify the risk-relevant signals present in the digest. Each signal \
            must be a short factual phrase grounded ONLY in the digest - never invent transactions, \
            amounts or counterparties that are not listed.
            2. retrievalQueries: formulate 2 to 6 short keyword-style search queries for the bank's \
            internal policy knowledge base, such that the documents retrieved would let an analyst \
            evaluate every salient signal. The knowledge base covers: AML transaction monitoring, \
            high-risk jurisdictions, crypto-asset policy, card fraud guidance, structuring red flags, \
            escalation/SAR procedure, and customer risk classification.

            Also include one query about risk level classification criteria, so the final grading can \
            be policy-based. If the digest shows no risk signals at all, say so in salientSignals \
            (e.g. "no automated rule fired; activity consistent with profile") and still return \
            queries for baseline classification and monitoring policy.""";

    static final String ANALYSIS_SYSTEM = """
            You are a senior financial-crime analyst producing a risk analysis for a bank \
            customer-care operator reviewing one customer's activity.

            You will receive:
            - ACTIVITY DIGEST: a compact, complete summary of the customer's card, payment and \
            crypto activity, including which automated risk rules fired and notable transactions.
            - POLICY EXCERPTS: numbered excerpts [S1], [S2], ... retrieved from the bank's internal \
            policy knowledge base.

            Produce a risk analysis with riskLevel, summary, findings and recommendations. Rules:
            - Ground every statement in the ACTIVITY DIGEST and POLICY EXCERPTS only. Never invent \
            transactions, amounts, dates, counterparties or policy rules. If evidence is \
            insufficient for a conclusion, say so instead of speculating.
            - Grade riskLevel using the classification criteria in the policy excerpts (LOW, MEDIUM, \
            HIGH, CRITICAL). Combined independent signal families outweigh repetition of one family. \
            Failed and reversed transactions count as behaviour.
            - Every finding must cite its evidence (specific transactions, amounts, dates or \
            patterns from the digest) and list the policy excerpts it relies on in policyRefs, using \
            the excerpt's document and section title, e.g. \
            "Crypto-Asset Activity Policy - Mixing and tumbling services".
            - recommendations must be concrete next actions available to a customer-care operator \
            and consistent with the escalation paths in the policy excerpts. Order them by priority. \
            Remember the tipping-off prohibition: never recommend telling the customer they are \
            under suspicion or that a report may be filed.
            - Write for a professional operator: factual, concise, no speculation about intent \
            ("patterns consistent with structuring", not "the customer is laundering money").""";

    public String triageSystemPrompt() {
        return TRIAGE_SYSTEM;
    }

    public String analysisSystemPrompt() {
        return ANALYSIS_SYSTEM;
    }

    public String triageUserPrompt(ActivityDigest digest) {
        return "ACTIVITY DIGEST\n" + renderDigest(digest);
    }

    public String analysisUserPrompt(ActivityDigest digest, List<RetrievedChunk> chunks, String repairFeedback) {
        StringBuilder sb = new StringBuilder();
        sb.append("ACTIVITY DIGEST\n").append(renderDigest(digest));
        sb.append("\nPOLICY EXCERPTS\n");
        if (chunks.isEmpty()) {
            sb.append("(no policy excerpts retrieved)\n");
        }
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            sb.append("[S").append(i + 1).append("] ")
                    .append(chunk.documentTitle());
            if (chunk.sectionTitle() != null) {
                sb.append(" - ").append(chunk.sectionTitle());
            }
            sb.append('\n').append(chunk.content()).append("\n\n");
        }
        if (repairFeedback != null) {
            sb.append("\nIMPORTANT - your previous answer was rejected by validation: ")
                    .append(repairFeedback)
                    .append("\nProduce a corrected analysis that fixes this.\n");
        }
        return sb.toString();
    }

    /** Renders the digest as compact structured text (stable order, no raw rows). */
    public String renderDigest(ActivityDigest d) {
        StringBuilder sb = new StringBuilder();
        sb.append("customer: ").append(d.fullName())
                .append(" | segment: ").append(d.segment())
                .append(" | residence country: ").append(d.country())
                .append(" | customer since: ").append(d.customerSince().toLocalDate())
                .append('\n');
        sb.append("activity window: ")
                .append(d.firstActivityAt() == null ? "n/a" : TS.format(d.firstActivityAt()))
                .append(" to ")
                .append(d.lastActivityAt() == null ? "n/a" : TS.format(d.lastActivityAt()))
                .append(" | total transactions: ").append(d.totalTransactions())
                .append('\n');

        sb.append("\nactivity by type:\n");
        if (d.typeSummaries().isEmpty()) {
            sb.append("  (no activity)\n");
        }
        for (TypeSummary s : d.typeSummaries()) {
            sb.append("  ").append(s.type()).append(": ").append(s.count()).append(" tx")
                    .append(" (completed ").append(s.completed())
                    .append(", pending ").append(s.pending())
                    .append(", failed ").append(s.failed())
                    .append(", reversed ").append(s.reversed()).append(")")
                    .append(" | completed totals: ").append(s.completedTotalsByCurrency())
                    .append('\n');
        }

        if (!d.paymentCorridors().isEmpty()) {
            sb.append("\npayment corridors (method->receiver country: count): ")
                    .append(d.paymentCorridors()).append('\n');
        }
        if (!d.cardMccCounts().isEmpty()) {
            sb.append("card merchant categories (MCC: count): ").append(d.cardMccCounts()).append('\n');
        }
        if (!d.cryptoCounterparties().isEmpty()) {
            sb.append("crypto counterparties: ").append(String.join("; ", d.cryptoCounterparties())).append('\n');
        }

        sb.append("\nautomated risk rules fired (aggregate score ").append(d.totalRiskScore()).append("):\n");
        if (d.ruleFires().isEmpty()) {
            sb.append("  none\n");
        }
        for (RuleFire rf : d.ruleFires()) {
            sb.append("  - ").append(rf.ruleName())
                    .append(" [").append(rf.appliesTo()).append("]")
                    .append(" fired ").append(rf.timesFired()).append("x")
                    .append(", score contribution ").append(rf.totalContribution())
                    .append(" (rule logic: ").append(rf.thresholdLogic()).append(")")
                    .append('\n');
        }

        sb.append("\nnotable transactions (all rule-flagged tx plus largest for context, newest first):\n");
        if (d.notableTransactions().isEmpty()) {
            sb.append("  none\n");
        }
        for (NotableTransaction nt : d.notableTransactions()) {
            sb.append("  - ").append(TS.format(nt.at()))
                    .append(' ').append(nt.type())
                    .append(' ').append(nt.amount()).append(' ').append(nt.currency())
                    .append(' ').append(nt.status())
                    .append(" | ").append(nt.descriptor());
            if (!nt.firedRuleNames().isEmpty()) {
                sb.append(" | rules: ").append(String.join(", ", nt.firedRuleNames()));
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
