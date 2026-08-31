package com.assignment.analytics.ai.llm;

import com.assignment.analytics.ai.ActivityDigest;
import com.assignment.analytics.ai.model.RiskAnalysisResult;
import com.assignment.analytics.ai.model.TriagePlan;
import com.assignment.analytics.domain.LlmMode;
import com.assignment.analytics.rag.RetrievedChunk;

import java.util.List;

/**
 * Abstraction over the LLM backend. Two implementations exist:
 * {@link AnthropicLlmClient} (real calls to Claude) and
 * {@link StubLlmClient} (deterministic offline stub). Both receive the same
 * inputs and honour the same output contracts, so the analysis pipeline -
 * digest, retrieval, validation, persistence - is identical in either mode.
 */
public interface LlmClient {

    LlmMode mode();

    String modelName();

    /**
     * LLM call 1 - triage & retrieval planning: reads the activity digest and
     * returns the salient risk signals plus the queries to run against the
     * policy knowledge base.
     */
    LlmInvocation<TriagePlan> triage(ActivityDigest digest, String systemPrompt, String userPrompt);

    /**
     * LLM call 2 - risk analysis: reads the digest plus retrieved policy
     * excerpts and returns the graded, grounded analysis.
     */
    LlmInvocation<RiskAnalysisResult> analyze(ActivityDigest digest, List<RetrievedChunk> chunks,
                                              String systemPrompt, String userPrompt);

    /** Result of one LLM invocation, with token usage when the backend reports it. */
    record LlmInvocation<T>(T output, Long inputTokens, Long outputTokens) {
    }
}
