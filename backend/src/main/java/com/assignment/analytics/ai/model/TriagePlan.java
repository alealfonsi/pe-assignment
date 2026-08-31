package com.assignment.analytics.ai.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Output contract of LLM call 1 ("triage & retrieval planning").
 * The record doubles as the JSON schema the model output is constrained to
 * (structured outputs derive the schema from this class).
 */
public record TriagePlan(
        @JsonPropertyDescription("Salient risk signals observed in the activity digest, each a short factual phrase grounded in the provided data")
        List<String> salientSignals,
        @JsonPropertyDescription("2 to 6 short keyword-style search queries for the internal policy knowledge base, covering every salient signal")
        List<String> retrievalQueries) {
}
