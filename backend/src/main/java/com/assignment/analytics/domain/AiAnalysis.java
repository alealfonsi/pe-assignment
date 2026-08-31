package com.assignment.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persisted result of one AI-powered analysis run, including the retrieval
 * context (queries + policy excerpts) so the record is auditable later.
 */
@Entity
@Table(name = "ai_analyses")
public class AiAnalysis {

    @Id
    @Column(name = "analysis_id")
    private UUID analysisId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "llm_mode", nullable = false)
    private LlmMode llmMode;

    @Column(nullable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private String summary;

    @Column(name = "findings_json", nullable = false)
    private String findingsJson;

    @Column(name = "recommendations_json", nullable = false)
    private String recommendationsJson;

    @Column(name = "salient_signals_json", nullable = false)
    private String salientSignalsJson;

    @Column(name = "retrieval_queries_json", nullable = false)
    private String retrievalQueriesJson;

    @Column(name = "retrieved_chunks_json", nullable = false)
    private String retrievedChunksJson;

    @Column(name = "input_tokens")
    private Long inputTokens;

    @Column(name = "output_tokens")
    private Long outputTokens;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    protected AiAnalysis() {
    }

    public AiAnalysis(UUID analysisId, UUID customerId, UUID operatorId, LocalDateTime createdAt,
                      LlmMode llmMode, String model, RiskLevel riskLevel, String summary,
                      String findingsJson, String recommendationsJson, String salientSignalsJson,
                      String retrievalQueriesJson, String retrievedChunksJson,
                      Long inputTokens, Long outputTokens, long latencyMs) {
        this.analysisId = analysisId;
        this.customerId = customerId;
        this.operatorId = operatorId;
        this.createdAt = createdAt;
        this.llmMode = llmMode;
        this.model = model;
        this.riskLevel = riskLevel;
        this.summary = summary;
        this.findingsJson = findingsJson;
        this.recommendationsJson = recommendationsJson;
        this.salientSignalsJson = salientSignalsJson;
        this.retrievalQueriesJson = retrievalQueriesJson;
        this.retrievedChunksJson = retrievedChunksJson;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.latencyMs = latencyMs;
    }

    public UUID getAnalysisId() {
        return analysisId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getOperatorId() {
        return operatorId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LlmMode getLlmMode() {
        return llmMode;
    }

    public String getModel() {
        return model;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getSummary() {
        return summary;
    }

    public String getFindingsJson() {
        return findingsJson;
    }

    public String getRecommendationsJson() {
        return recommendationsJson;
    }

    public String getSalientSignalsJson() {
        return salientSignalsJson;
    }

    public String getRetrievalQueriesJson() {
        return retrievalQueriesJson;
    }

    public String getRetrievedChunksJson() {
        return retrievedChunksJson;
    }

    public Long getInputTokens() {
        return inputTokens;
    }

    public Long getOutputTokens() {
        return outputTokens;
    }

    public long getLatencyMs() {
        return latencyMs;
    }
}
