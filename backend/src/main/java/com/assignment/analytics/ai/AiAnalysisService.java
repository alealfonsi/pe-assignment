package com.assignment.analytics.ai;

import com.assignment.analytics.ai.AnalysisDtos.AnalysisResponse;
import com.assignment.analytics.ai.AnalysisDtos.SourceRef;
import com.assignment.analytics.ai.llm.LlmClient;
import com.assignment.analytics.ai.llm.LlmClient.LlmInvocation;
import com.assignment.analytics.ai.llm.LlmException;
import com.assignment.analytics.ai.model.RiskAnalysisResult;
import com.assignment.analytics.ai.model.TriagePlan;
import com.assignment.analytics.auth.AuthenticatedOperator;
import com.assignment.analytics.common.NotFoundException;
import com.assignment.analytics.config.AiProperties;
import com.assignment.analytics.domain.AiAnalysis;
import com.assignment.analytics.domain.Operator;
import com.assignment.analytics.rag.PolicyRetriever;
import com.assignment.analytics.rag.RetrievedChunk;
import com.assignment.analytics.repo.AiAnalysisRepository;
import com.assignment.analytics.repo.OperatorRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the AI analysis pipeline:
 *
 *   1. deterministic activity digest (no LLM)
 *   2. LLM call 1: triage & retrieval planning (structured output)
 *   3. BM25 retrieval over the policy corpus (no LLM)
 *   4. LLM call 2: grounded risk analysis (structured output)
 *   5. semantic validation, with at most one repair re-call on failure
 *   6. persistence of the full, auditable result
 */
@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);
    private static final int PERSISTED_EXCERPT_MAX_CHARS = 500;

    private final ActivityDigestBuilder digestBuilder;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final PolicyRetriever retriever;
    private final AiAnalysisRepository analyses;
    private final OperatorRepository operators;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public AiAnalysisService(ActivityDigestBuilder digestBuilder, PromptBuilder promptBuilder,
                             LlmClient llmClient, PolicyRetriever retriever, AiAnalysisRepository analyses,
                             OperatorRepository operators, AiProperties properties, ObjectMapper objectMapper) {
        this.digestBuilder = digestBuilder;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.retriever = retriever;
        this.analyses = analyses;
        this.operators = operators;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public AnalysisResponse run(UUID customerId, AuthenticatedOperator operator) {
        long startNanos = System.nanoTime();

        // 1. Deterministic digest - the single source of facts for everything below.
        ActivityDigest digest = digestBuilder.build(customerId);

        // 2. LLM call 1: identify salient signals and plan the policy-KB queries.
        LlmInvocation<TriagePlan> triage = llmClient.triage(digest,
                promptBuilder.triageSystemPrompt(), promptBuilder.triageUserPrompt(digest));
        TriagePlan plan = sanitizeTriage(triage.output());

        // 3. Retrieval: BM25 over policy chunks with the LLM-formulated queries.
        List<RetrievedChunk> chunks = retriever.retrieve(plan.retrievalQueries(),
                properties.retrieval().topK());

        // 4. LLM call 2: the grounded analysis.
        LlmInvocation<RiskAnalysisResult> analysis = llmClient.analyze(digest, chunks,
                promptBuilder.analysisSystemPrompt(),
                promptBuilder.analysisUserPrompt(digest, chunks, null));

        // 5. Validate; on failure, one repair call carrying the validation feedback.
        List<String> problems = validate(analysis.output());
        Long extraIn = null;
        Long extraOut = null;
        if (!problems.isEmpty()) {
            String feedback = String.join("; ", problems);
            log.warn("Analysis output failed validation ({}); issuing repair call", feedback);
            extraIn = analysis.inputTokens();
            extraOut = analysis.outputTokens();
            analysis = llmClient.analyze(digest, chunks,
                    promptBuilder.analysisSystemPrompt(),
                    promptBuilder.analysisUserPrompt(digest, chunks, feedback));
            List<String> remaining = validate(analysis.output());
            if (!remaining.isEmpty()) {
                throw new LlmException("Analysis output failed validation after repair: "
                        + String.join("; ", remaining));
            }
        }

        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
        RiskAnalysisResult result = analysis.output();
        List<SourceRef> sources = chunks.stream()
                .map(c -> new SourceRef(c.documentTitle(), c.sectionTitle(), truncate(c.content())))
                .toList();

        // 6. Persist the full auditable record.
        AiAnalysis entity = new AiAnalysis(
                UUID.randomUUID(), customerId, operator.operatorId(), LocalDateTime.now(),
                llmClient.mode(), llmClient.modelName(), result.riskLevel(), result.summary(),
                toJson(result.findings()), toJson(result.recommendations()),
                toJson(plan.salientSignals()), toJson(plan.retrievalQueries()), toJson(sources),
                sum(triage.inputTokens(), analysis.inputTokens(), extraIn),
                sum(triage.outputTokens(), analysis.outputTokens(), extraOut),
                latencyMs);
        analyses.save(entity);

        return toResponse(entity, operator.displayName());
    }

    public List<AnalysisResponse> history(UUID customerId) {
        return analyses.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(a -> toResponse(a, operatorName(a.getOperatorId())))
                .toList();
    }

    public AnalysisResponse get(UUID customerId, UUID analysisId) {
        AiAnalysis analysis = analyses.findById(analysisId)
                .filter(a -> a.getCustomerId().equals(customerId))
                .orElseThrow(() -> new NotFoundException("Analysis not found: " + analysisId));
        return toResponse(analysis, operatorName(analysis.getOperatorId()));
    }

    /** Keeps the pipeline robust to a degenerate triage output. */
    private TriagePlan sanitizeTriage(TriagePlan plan) {
        List<String> signals = plan.salientSignals() == null ? List.of() : plan.salientSignals();
        List<String> queries = plan.retrievalQueries() == null ? new ArrayList<>() : new ArrayList<>(plan.retrievalQueries());
        queries.removeIf(q -> q == null || q.isBlank());
        if (queries.isEmpty()) {
            queries = List.of("customer risk classification levels criteria",
                    "transaction monitoring risk-based approach");
        }
        return new TriagePlan(signals, List.copyOf(queries));
    }

    /** Semantic validation on top of the schema-constrained output. */
    static List<String> validate(RiskAnalysisResult result) {
        List<String> problems = new ArrayList<>();
        if (result == null) {
            return List.of("no analysis produced");
        }
        if (result.riskLevel() == null) {
            problems.add("riskLevel is missing (must be LOW, MEDIUM, HIGH or CRITICAL)");
        }
        if (result.summary() == null || result.summary().isBlank()) {
            problems.add("summary is empty");
        }
        if (result.findings() == null) {
            problems.add("findings is missing (use an empty list when there is nothing to report)");
        } else {
            for (RiskAnalysisResult.Finding finding : result.findings()) {
                if (finding.title() == null || finding.title().isBlank()
                        || finding.evidence() == null || finding.evidence().isBlank()
                        || finding.severity() == null) {
                    problems.add("every finding needs a title, a severity and concrete evidence");
                    break;
                }
            }
        }
        if (result.recommendations() == null || result.recommendations().isEmpty()) {
            problems.add("recommendations must contain at least one actionable item");
        }
        return problems;
    }

    private AnalysisResponse toResponse(AiAnalysis a, String operatorName) {
        return new AnalysisResponse(
                a.getAnalysisId(), a.getCustomerId(), operatorName, a.getCreatedAt(),
                a.getLlmMode(), a.getModel(), a.getRiskLevel(), a.getSummary(),
                fromJson(a.getFindingsJson(), new TypeReference<List<RiskAnalysisResult.Finding>>() {
                }),
                fromJson(a.getRecommendationsJson(), new TypeReference<List<String>>() {
                }),
                fromJson(a.getSalientSignalsJson(), new TypeReference<List<String>>() {
                }),
                fromJson(a.getRetrievalQueriesJson(), new TypeReference<List<String>>() {
                }),
                fromJson(a.getRetrievedChunksJson(), new TypeReference<List<SourceRef>>() {
                }),
                a.getInputTokens(), a.getOutputTokens(), a.getLatencyMs());
    }

    private String operatorName(UUID operatorId) {
        return operators.findById(operatorId).map(Operator::getDisplayName).orElse("unknown");
    }

    private static String truncate(String content) {
        return content.length() <= PERSISTED_EXCERPT_MAX_CHARS
                ? content
                : content.substring(0, PERSISTED_EXCERPT_MAX_CHARS) + "…";
    }

    private static Long sum(Long... values) {
        Long total = null;
        for (Long value : values) {
            if (value != null) {
                total = (total == null ? 0L : total) + value;
            }
        }
        return total;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize analysis payload", e);
        }
    }

    private <T> T fromJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize stored analysis payload", e);
        }
    }
}
