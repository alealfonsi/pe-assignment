package com.assignment.analytics.ai;

import com.assignment.analytics.domain.ActivityType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Deterministic, compact representation of a customer's activity, produced by
 * {@link ActivityDigestBuilder} without any LLM involvement. It is the single
 * source of facts for both LLM calls (rendered to text by PromptBuilder) and
 * for the stub pipeline (consumed structurally).
 */
public record ActivityDigest(
        UUID customerId,
        String fullName,
        String segment,
        String country,
        LocalDateTime customerSince,
        LocalDateTime firstActivityAt,
        LocalDateTime lastActivityAt,
        int totalTransactions,
        List<TypeSummary> typeSummaries,
        List<RuleFire> ruleFires,
        BigDecimal totalRiskScore,
        Map<String, Long> paymentCorridors,
        Map<String, Long> cardMccCounts,
        List<String> cryptoCounterparties,
        List<NotableTransaction> notableTransactions) {

    public record TypeSummary(ActivityType type, long count, Map<String, BigDecimal> completedTotalsByCurrency,
                              long completed, long pending, long failed, long reversed) {
    }

    public record RuleFire(String ruleName, String appliesTo, String thresholdLogic, BigDecimal weight,
                           long timesFired, BigDecimal totalContribution) {
    }

    public record NotableTransaction(UUID transactionId, ActivityType type, LocalDateTime at, BigDecimal amount,
                                     String currency, String status, String descriptor,
                                     List<String> firedRuleNames) {
    }

    public boolean hasRuleFires() {
        return !ruleFires.isEmpty();
    }
}
