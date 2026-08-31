package com.assignment.analytics.ai;

import com.assignment.analytics.ai.llm.LlmClient;
import com.assignment.analytics.ai.llm.LlmException;
import com.assignment.analytics.ai.model.RiskAnalysisResult;
import com.assignment.analytics.ai.model.TriagePlan;
import com.assignment.analytics.auth.AuthenticatedOperator;
import com.assignment.analytics.config.AiProperties;
import com.assignment.analytics.domain.AiAnalysis;
import com.assignment.analytics.domain.LlmMode;
import com.assignment.analytics.domain.RiskLevel;
import com.assignment.analytics.rag.PolicyRetriever;
import com.assignment.analytics.rag.RetrievedChunk;
import com.assignment.analytics.repo.AiAnalysisRepository;
import com.assignment.analytics.repo.OperatorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises orchestration concerns - validation, the single repair retry, and
 * persistence - against a scripted fake LlmClient.
 */
class AiAnalysisServiceTest {

    private final UUID customerId = UUID.randomUUID();
    private final AuthenticatedOperator operator =
            new AuthenticatedOperator(UUID.randomUUID(), "alice", "Alice", "OPERATOR");

    private ActivityDigestBuilder digestBuilder;
    private PolicyRetriever retriever;
    private AiAnalysisRepository analysisRepository;
    private OperatorRepository operatorRepository;

    private final ActivityDigest digest = new ActivityDigest(UUID.randomUUID(), "Cust", "RETAIL", "IT",
            LocalDateTime.now().minusYears(1), LocalDateTime.now().minusDays(30), LocalDateTime.now(),
            5, List.of(), List.of(), BigDecimal.ZERO, Map.of(), Map.of(), List.of(), List.of());

    private final TriagePlan plan = new TriagePlan(List.of("signal"), List.of("query one"));
    private final RiskAnalysisResult valid = new RiskAnalysisResult(RiskLevel.LOW, "All fine.",
            List.of(), List.of("Document the review outcome."));
    private final RiskAnalysisResult invalid = new RiskAnalysisResult(RiskLevel.LOW, "All fine.",
            List.of(), List.of());  // no recommendations -> fails validation

    @BeforeEach
    void setUp() {
        digestBuilder = mock(ActivityDigestBuilder.class);
        retriever = mock(PolicyRetriever.class);
        analysisRepository = mock(AiAnalysisRepository.class);
        operatorRepository = mock(OperatorRepository.class);
        when(digestBuilder.build(customerId)).thenReturn(digest);
        when(retriever.retrieve(anyList(), anyInt())).thenReturn(List.of(
                new RetrievedChunk(UUID.randomUUID(), "Doc", "Section", "content", 1.0)));
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private AiAnalysisService service(LlmClient llm) {
        return new AiAnalysisService(digestBuilder, new PromptBuilder(), llm, retriever,
                analysisRepository, operatorRepository,
                new AiProperties(AiProperties.Mode.STUB,
                        new AiProperties.Anthropic("claude-opus-5", 8192, 120),
                        new AiProperties.Retrieval(6)),
                new ObjectMapper());
    }

    private LlmClient scriptedLlm(RiskAnalysisResult first, RiskAnalysisResult second, AtomicInteger calls) {
        return new LlmClient() {
            @Override
            public LlmMode mode() {
                return LlmMode.STUB;
            }

            @Override
            public String modelName() {
                return "fake";
            }

            @Override
            public LlmInvocation<TriagePlan> triage(ActivityDigest d, String sys, String user) {
                return new LlmInvocation<>(plan, 10L, 5L);
            }

            @Override
            public LlmInvocation<RiskAnalysisResult> analyze(ActivityDigest d, List<RetrievedChunk> chunks,
                                                             String sys, String user) {
                int n = calls.incrementAndGet();
                if (n == 2) {
                    // The repair prompt must carry the validation feedback.
                    assertThat(user).contains("previous answer was rejected");
                }
                return new LlmInvocation<>(n == 1 ? first : second, 100L, 50L);
            }
        };
    }

    @Test
    void happyPathPersistsAndReturnsAnalysis() {
        AtomicInteger calls = new AtomicInteger();
        var response = service(scriptedLlm(valid, valid, calls)).run(customerId, operator);

        assertThat(calls.get()).isEqualTo(1);
        assertThat(response.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(response.operatorName()).isEqualTo("Alice");
        assertThat(response.sources()).hasSize(1);
        assertThat(response.retrievalQueries()).containsExactly("query one");
        // token usage aggregated across triage + analysis
        assertThat(response.inputTokens()).isEqualTo(110L);
        assertThat(response.outputTokens()).isEqualTo(55L);
    }

    @Test
    void invalidOutputTriggersExactlyOneRepairCall() {
        AtomicInteger calls = new AtomicInteger();
        var response = service(scriptedLlm(invalid, valid, calls)).run(customerId, operator);

        assertThat(calls.get()).isEqualTo(2);
        assertThat(response.riskLevel()).isEqualTo(RiskLevel.LOW);
        // repair call tokens are counted too: 10 + 100 + 100
        assertThat(response.inputTokens()).isEqualTo(210L);
    }

    @Test
    void invalidOutputAfterRepairFails() {
        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(() -> service(scriptedLlm(invalid, invalid, calls)).run(customerId, operator))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("after repair");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void validationCatchesMissingPieces() {
        assertThat(AiAnalysisService.validate(null)).isNotEmpty();
        assertThat(AiAnalysisService.validate(new RiskAnalysisResult(null, "s",
                List.of(), List.of("r")))).anySatisfy(p -> assertThat(p).contains("riskLevel"));
        assertThat(AiAnalysisService.validate(new RiskAnalysisResult(RiskLevel.LOW, " ",
                List.of(), List.of("r")))).anySatisfy(p -> assertThat(p).contains("summary"));
        assertThat(AiAnalysisService.validate(new RiskAnalysisResult(RiskLevel.LOW, "s",
                List.of(new RiskAnalysisResult.Finding("t", null, "", List.of())), List.of("r"))))
                .anySatisfy(p -> assertThat(p).contains("finding"));
        assertThat(AiAnalysisService.validate(valid)).isEmpty();
    }

    @Test
    void degenerateTriageQueriesFallBackToDefaults() {
        AtomicInteger calls = new AtomicInteger();
        LlmClient llm = new LlmClient() {
            @Override
            public LlmMode mode() {
                return LlmMode.STUB;
            }

            @Override
            public String modelName() {
                return "fake";
            }

            @Override
            public LlmInvocation<TriagePlan> triage(ActivityDigest d, String sys, String user) {
                return new LlmInvocation<>(new TriagePlan(null, List.of("  ")), null, null);
            }

            @Override
            public LlmInvocation<RiskAnalysisResult> analyze(ActivityDigest d, List<RetrievedChunk> chunks,
                                                             String sys, String user) {
                calls.incrementAndGet();
                return new LlmInvocation<>(valid, null, null);
            }
        };
        var response = service(llm).run(customerId, operator);
        assertThat(response.retrievalQueries())
                .contains("customer risk classification levels criteria");
        assertThat(response.inputTokens()).isNull();

        // persisted entity mirrors the response
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void persistedEntityCarriesAuditFields() {
        var service = service(scriptedLlm(valid, valid, new AtomicInteger()));
        var response = service.run(customerId, operator);

        org.mockito.ArgumentCaptor<AiAnalysis> captor = org.mockito.ArgumentCaptor.forClass(AiAnalysis.class);
        org.mockito.Mockito.verify(analysisRepository).save(captor.capture());
        AiAnalysis saved = captor.getValue();
        assertThat(saved.getAnalysisId()).isEqualTo(response.analysisId());
        assertThat(saved.getCustomerId()).isEqualTo(customerId);
        assertThat(saved.getOperatorId()).isEqualTo(operator.operatorId());
        assertThat(saved.getRetrievedChunksJson()).contains("Doc");
        assertThat(saved.getSalientSignalsJson()).contains("signal");
    }
}
