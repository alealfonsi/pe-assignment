package com.assignment.analytics.ai;

import com.assignment.analytics.ai.model.RiskAnalysisResult;
import com.assignment.analytics.domain.LlmMode;
import com.assignment.analytics.domain.RiskLevel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class AnalysisDtos {

    private AnalysisDtos() {
    }

    /** Policy excerpt reference persisted with, and returned alongside, an analysis. */
    public record SourceRef(String documentTitle, String sectionTitle, String excerpt) {
    }

    public record AnalysisResponse(
            UUID analysisId,
            UUID customerId,
            String operatorName,
            LocalDateTime createdAt,
            LlmMode llmMode,
            String model,
            RiskLevel riskLevel,
            String summary,
            List<RiskAnalysisResult.Finding> findings,
            List<String> recommendations,
            List<String> salientSignals,
            List<String> retrievalQueries,
            List<SourceRef> sources,
            Long inputTokens,
            Long outputTokens,
            long latencyMs) {
    }
}
