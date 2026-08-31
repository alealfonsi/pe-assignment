package com.assignment.analytics.ai.llm;

import com.assignment.analytics.ai.ActivityDigest;
import com.assignment.analytics.ai.ActivityDigest.NotableTransaction;
import com.assignment.analytics.ai.ActivityDigest.RuleFire;
import com.assignment.analytics.ai.model.RiskAnalysisResult;
import com.assignment.analytics.ai.model.RiskAnalysisResult.Finding;
import com.assignment.analytics.ai.model.TriagePlan;
import com.assignment.analytics.domain.LlmMode;
import com.assignment.analytics.domain.RiskLevel;
import com.assignment.analytics.rag.RetrievedChunk;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deterministic offline LLM backend. It consumes the same inputs and honours
 * the same output contracts as {@link AnthropicLlmClient}, driving the full
 * pipeline (triage -> retrieval -> analysis -> persistence) without network
 * access. The "reasoning" is rule-driven: risk signals and retrieval queries
 * are derived from the fired risk rules in the digest, and the grading follows
 * the thresholds written in the AML/classification policy documents, so stub
 * output stays consistent with what the policies say.
 */
public class StubLlmClient implements LlmClient {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Maps fired-rule names (by keyword) to policy KB retrieval queries. */
    private static final Map<String, String> RULE_QUERIES = Map.of(
            "cross-border", "high-value cross-border wire payment economic purpose",
            "jurisdiction", "high-risk jurisdictions enhanced monitoring greylist",
            "structuring", "structuring sub-threshold transfers red flags",
            "crypto outflow", "rapid crypto outflow unhosted wallet dispersal",
            "mixing", "mixing tumbling service escalation crypto",
            "exchange", "approved exchanges VASP list crypto",
            "decline", "card testing decline burst fraud",
            "merchant category", "high-risk merchant category gambling quasi-cash",
            "card-not-present", "card-not-present high value fraud",
            "velocity", "velocity spike dormant account baseline");

    private static final String ROUND_AMOUNT_QUERY = "round-amount pattern layering";

    @Override
    public LlmMode mode() {
        return LlmMode.STUB;
    }

    @Override
    public String modelName() {
        return "stub-deterministic-v1";
    }

    @Override
    public LlmInvocation<TriagePlan> triage(ActivityDigest digest, String systemPrompt, String userPrompt) {
        List<String> signals = new ArrayList<>();
        Set<String> queries = new LinkedHashSet<>();

        for (RuleFire fire : digest.ruleFires()) {
            signals.add(fire.ruleName() + " fired " + fire.timesFired() + "x (score " + fire.totalContribution() + ")");
            String nameLower = fire.ruleName().toLowerCase(Locale.ROOT);
            RULE_QUERIES.forEach((keyword, query) -> {
                if (nameLower.contains(keyword)) {
                    queries.add(query);
                }
            });
            if (nameLower.contains("round-amount")) {
                queries.add(ROUND_AMOUNT_QUERY);
            }
        }
        if (digest.ruleFires().isEmpty()) {
            signals.add("no automated risk rule fired; activity appears consistent with the customer profile");
            queries.add("transaction monitoring risk-based approach");
        }
        // Corridor context even when only the generic cross-border rule fired.
        digest.paymentCorridors().keySet().stream()
                .map(corridor -> corridor.substring(corridor.indexOf("->") + 2))
                .filter(country -> !country.equals(digest.country()))
                .findAny()
                .ifPresent(c -> queries.add("high-risk jurisdictions enhanced monitoring greylist"));
        // Always ground the grading and the recommended actions in policy.
        queries.add("customer risk classification levels criteria");
        queries.add("escalation paths EDD referral SAR");

        return new LlmInvocation<>(new TriagePlan(signals, List.copyOf(queries)), null, null);
    }

    @Override
    public LlmInvocation<RiskAnalysisResult> analyze(ActivityDigest digest, List<RetrievedChunk> chunks,
                                                     String systemPrompt, String userPrompt) {
        boolean mixerInvolved = digest.ruleFires().stream()
                .anyMatch(f -> f.ruleName().toLowerCase(Locale.ROOT).contains("mixing"));
        RiskLevel level = grade(digest.totalRiskScore(), mixerInvolved, digest.ruleFires().size());

        List<Finding> findings = digest.ruleFires().stream()
                .map(fire -> toFinding(fire, digest, chunks))
                .toList();

        return new LlmInvocation<>(new RiskAnalysisResult(
                level,
                buildSummary(digest, level),
                findings,
                recommendations(level, mixerInvolved)), null, null);
    }

    /**
     * Grading mirrors the AML Transaction Monitoring Policy score bands
     * (0-19 routine, 20-49 heightened, 50-89 EDD, 90+ escalate) with the
     * mixer-exposure critical override from the Crypto-Asset Activity Policy.
     */
    private RiskLevel grade(BigDecimal totalScore, boolean mixerInvolved, int distinctRules) {
        if (mixerInvolved || totalScore.compareTo(BigDecimal.valueOf(90)) >= 0) {
            return RiskLevel.CRITICAL;
        }
        if (totalScore.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return RiskLevel.HIGH;
        }
        if (totalScore.compareTo(BigDecimal.valueOf(20)) >= 0 || distinctRules >= 2) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private String buildSummary(ActivityDigest digest, RiskLevel level) {
        StringBuilder sb = new StringBuilder();
        sb.append(digest.fullName()).append(" (").append(digest.segment()).append(", ")
                .append(digest.country()).append(") has ").append(digest.totalTransactions())
                .append(" transactions in the review window");
        if (digest.firstActivityAt() != null) {
            sb.append(" (").append(digest.firstActivityAt().toLocalDate()).append(" to ")
                    .append(digest.lastActivityAt().toLocalDate()).append(")");
        }
        sb.append(". ");
        if (digest.ruleFires().isEmpty()) {
            sb.append("No automated risk rule fired and the activity mix appears consistent with the profile. ");
        } else {
            sb.append("Automated monitoring fired ").append(digest.ruleFires().size())
                    .append(" distinct rule(s) with an aggregate score of ").append(digest.totalRiskScore())
                    .append("; the principal driver is \"").append(digest.ruleFires().get(0).ruleName())
                    .append("\". ");
        }
        sb.append("Overall risk level assessed as ").append(level).append(" per the Customer Risk ")
                .append("Classification Standard and the AML monitoring score bands.");
        return sb.toString();
    }

    private Finding toFinding(RuleFire fire, ActivityDigest digest, List<RetrievedChunk> chunks) {
        String evidence = digest.notableTransactions().stream()
                .filter(nt -> nt.firedRuleNames().contains(fire.ruleName()))
                .limit(4)
                .map(this::describeTransaction)
                .collect(Collectors.joining("; "));
        if (evidence.isEmpty()) {
            evidence = "Rule fired " + fire.timesFired() + " time(s), total score contribution "
                    + fire.totalContribution();
        }
        return new Finding(fire.ruleName(), severityFor(fire), evidence, matchPolicyRefs(fire, chunks));
    }

    private String describeTransaction(NotableTransaction nt) {
        return TS.format(nt.at()) + " " + nt.type() + " " + nt.amount() + " " + nt.currency()
                + " " + nt.status() + " (" + nt.descriptor() + ")";
    }

    private RiskLevel severityFor(RuleFire fire) {
        String name = fire.ruleName().toLowerCase(Locale.ROOT);
        if (name.contains("mixing")) {
            return RiskLevel.CRITICAL;
        }
        BigDecimal total = fire.totalContribution();
        if (total.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return RiskLevel.HIGH;
        }
        if (total.compareTo(BigDecimal.valueOf(20)) >= 0) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    /** Links a finding to retrieved policy excerpts by keyword overlap between rule name and chunk text. */
    private List<String> matchPolicyRefs(RuleFire fire, List<RetrievedChunk> chunks) {
        List<String> keywords = keywordsFor(fire.ruleName());
        Map<String, Integer> refScores = new LinkedHashMap<>();
        for (RetrievedChunk chunk : chunks) {
            String haystack = (chunk.documentTitle() + " " + chunk.sectionTitle() + " " + chunk.content())
                    .toLowerCase(Locale.ROOT);
            int hits = (int) keywords.stream().filter(haystack::contains).count();
            if (hits > 0) {
                String ref = chunk.sectionTitle() == null
                        ? chunk.documentTitle()
                        : chunk.documentTitle() + " - " + chunk.sectionTitle();
                refScores.merge(ref, hits, Integer::sum);
            }
        }
        return refScores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(2)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<String> keywordsFor(String ruleName) {
        String name = ruleName.toLowerCase(Locale.ROOT);
        List<String> keywords = new ArrayList<>();
        if (name.contains("cross-border")) {
            keywords.addAll(List.of("cross-border", "wire"));
        }
        if (name.contains("jurisdiction")) {
            keywords.addAll(List.of("jurisdiction", "greylist"));
        }
        if (name.contains("structuring")) {
            keywords.addAll(List.of("structuring", "threshold"));
        }
        if (name.contains("crypto outflow") || name.contains("rapid crypto")) {
            keywords.addAll(List.of("outflow", "unhosted"));
        }
        if (name.contains("mixing")) {
            keywords.addAll(List.of("mixing", "tumbling", "mixer"));
        }
        if (name.contains("exchange")) {
            keywords.addAll(List.of("exchange", "vasp"));
        }
        if (name.contains("decline")) {
            keywords.addAll(List.of("decline", "testing"));
        }
        if (name.contains("merchant category")) {
            keywords.addAll(List.of("mcc", "gambling"));
        }
        if (name.contains("card-not-present")) {
            keywords.addAll(List.of("card-not-present", "cnp"));
        }
        if (name.contains("velocity")) {
            keywords.addAll(List.of("velocity", "dormant"));
        }
        if (name.contains("round-amount")) {
            keywords.addAll(List.of("round", "layering"));
        }
        if (keywords.isEmpty()) {
            keywords.add("monitoring");
        }
        return keywords;
    }

    private List<String> recommendations(RiskLevel level, boolean mixerInvolved) {
        return switch (level) {
            case LOW -> List.of(
                    "Document the review outcome with a short rationale (document-and-monitor path).",
                    "No referral required; continue routine automated monitoring.");
            case MEDIUM -> List.of(
                    "Record a case note documenting each fired rule with transaction references.",
                    "Request supporting documentation from the customer through approved channels where "
                            + "an economic purpose is unclear.",
                    "Re-review the customer's activity within 30 days for repetition of the flagged patterns.");
            case HIGH -> List.of(
                    "Open an enhanced due diligence (EDD) referral to the Financial Crime team within "
                            + "5 business days, attaching this analysis and the flagged transaction references.",
                    "Verify source of funds and documented economic purpose for the high-value flagged transfers.",
                    "Do not disclose the review to the customer (tipping-off prohibition).");
            case CRITICAL -> mixerInvolved
                    ? List.of(
                    "Escalate to the Financial Crime team the same day (immediate escalation path).",
                    "Freeze further crypto withdrawals pending review, per the Crypto-Asset Activity Policy; "
                            + "use approved communication template CRY-14 only.",
                    "Preserve all transaction records for the SAR evaluation; do not inform the customer of "
                            + "the suspicion (tipping-off prohibition).")
                    : List.of(
                    "Escalate to the Financial Crime team the same day (immediate escalation path).",
                    "Evaluate feature restrictions pending review with supervisor approval.",
                    "Preserve all transaction records for the SAR evaluation; do not inform the customer of "
                            + "the suspicion (tipping-off prohibition).");
        };
    }
}
