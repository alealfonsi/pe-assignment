package com.assignment.analytics.ai.model;

import com.assignment.analytics.domain.RiskLevel;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Output contract of LLM call 2 ("risk analysis"). The record doubles as the
 * JSON schema the model output is constrained to (structured outputs).
 */
public record RiskAnalysisResult(
        @JsonPropertyDescription("Overall risk level of the reviewed activity, graded per the Customer Risk Classification Standard")
        RiskLevel riskLevel,
        @JsonPropertyDescription("3-6 sentence executive summary for the customer-care operator, naming the principal risk driver")
        String summary,
        @JsonPropertyDescription("Concrete findings, each tied to transactions or patterns present in the provided data")
        List<Finding> findings,
        @JsonPropertyDescription("Ordered, actionable recommendations available to a customer-care operator, consistent with the escalation policy excerpts")
        List<String> recommendations) {

    public record Finding(
            @JsonPropertyDescription("Short finding title")
            String title,
            @JsonPropertyDescription("Severity of this individual finding")
            RiskLevel severity,
            @JsonPropertyDescription("Evidence: the specific transactions, amounts, dates or patterns from the provided data supporting this finding")
            String evidence,
            @JsonPropertyDescription("Titles/sections of the provided policy excerpts this finding relies on, e.g. 'Crypto-Asset Activity Policy - Mixing and tumbling services'")
            List<String> policyRefs) {
    }
}
