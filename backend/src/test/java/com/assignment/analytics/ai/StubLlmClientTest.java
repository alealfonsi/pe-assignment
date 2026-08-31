package com.assignment.analytics.ai;

import com.assignment.analytics.ai.llm.StubLlmClient;
import com.assignment.analytics.ai.model.RiskAnalysisResult;
import com.assignment.analytics.ai.model.TriagePlan;
import com.assignment.analytics.domain.ActivityType;
import com.assignment.analytics.domain.RiskLevel;
import com.assignment.analytics.rag.RetrievedChunk;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StubLlmClientTest {

    private final StubLlmClient stub = new StubLlmClient();

    private ActivityDigest digest(BigDecimal totalScore, List<ActivityDigest.RuleFire> fires) {
        return new ActivityDigest(UUID.randomUUID(), "Test Customer", "RETAIL", "IT",
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2026, 8, 1, 0, 0), LocalDateTime.of(2026, 8, 30, 0, 0),
                10, List.of(), fires, totalScore, Map.of(), Map.of(), List.of(),
                List.of(new ActivityDigest.NotableTransaction(UUID.randomUUID(), ActivityType.CRYPTO,
                        LocalDateTime.of(2026, 8, 20, 2, 55), new BigDecimal("3.20"), "ETH", "COMPLETED",
                        "ETH 0xabc -> 0xmixer (no exchange)",
                        fires.isEmpty() ? List.of() : List.of(fires.get(0).ruleName()))));
    }

    private ActivityDigest.RuleFire fire(String name, int times, int total) {
        return new ActivityDigest.RuleFire(name, "ALL", "logic", BigDecimal.TEN, times,
                BigDecimal.valueOf(total));
    }

    @Test
    void triageMapsFiredRulesToRetrievalQueriesAndAlwaysAddsGroundingQueries() {
        TriagePlan plan = stub.triage(
                digest(new BigDecimal("70"), List.of(fire("Transfer to known mixing service", 2, 70))),
                "sys", "user").output();

        assertThat(plan.salientSignals()).anySatisfy(s -> assertThat(s).contains("mixing service"));
        assertThat(plan.retrievalQueries()).contains("mixing tumbling service escalation crypto",
                "customer risk classification levels criteria", "escalation paths EDD referral SAR");
    }

    @Test
    void triageOnCleanCustomerStillQueriesBaselinePolicies() {
        TriagePlan plan = stub.triage(digest(BigDecimal.ZERO, List.of()), "sys", "user").output();
        assertThat(plan.salientSignals()).hasSize(1);
        assertThat(plan.salientSignals().get(0)).contains("no automated risk rule fired");
        assertThat(plan.retrievalQueries()).isNotEmpty();
    }

    @Test
    void mixerExposureGradesCritical() {
        RiskAnalysisResult result = stub.analyze(
                digest(new BigDecimal("35"), List.of(fire("Transfer to known mixing service", 1, 35))),
                List.of(), "sys", "user").output();
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void gradesFollowPolicyScoreBands() {
        assertThat(stub.analyze(digest(BigDecimal.ZERO, List.of()), List.of(), "s", "u")
                .output().riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(stub.analyze(digest(new BigDecimal("25"),
                        List.of(fire("High-value cross-border payment", 1, 25))), List.of(), "s", "u")
                .output().riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(stub.analyze(digest(new BigDecimal("75"),
                        List.of(fire("High-value cross-border payment", 3, 75))), List.of(), "s", "u")
                .output().riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(stub.analyze(digest(new BigDecimal("120"),
                        List.of(fire("High-value cross-border payment", 5, 120))), List.of(), "s", "u")
                .output().riskLevel()).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void findingsCiteEvidenceAndRetrievedPolicySections() {
        RetrievedChunk chunk = new RetrievedChunk(UUID.randomUUID(), "Crypto-Asset Activity Policy",
                "Mixing and tumbling services", "mixer tumbling escalation text", 5.0);
        RiskAnalysisResult result = stub.analyze(
                digest(new BigDecimal("70"), List.of(fire("Transfer to known mixing service", 2, 70))),
                List.of(chunk), "sys", "user").output();

        assertThat(result.findings()).hasSize(1);
        RiskAnalysisResult.Finding finding = result.findings().get(0);
        assertThat(finding.evidence()).contains("2026-08-20 02:55");
        assertThat(finding.policyRefs())
                .contains("Crypto-Asset Activity Policy - Mixing and tumbling services");
        assertThat(result.recommendations()).anySatisfy(r -> assertThat(r).contains("same day"));
    }
}
