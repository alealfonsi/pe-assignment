package com.assignment.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI pipeline configuration.
 *
 * mode:
 *  - AUTO      use Anthropic when an API key is configured, otherwise the stub
 *  - ANTHROPIC require the real LLM (startup fails without an API key)
 *  - STUB      always use the deterministic stub
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(Mode mode, Anthropic anthropic, Retrieval retrieval) {

    public enum Mode {AUTO, ANTHROPIC, STUB}

    public record Anthropic(String model, long maxOutputTokens, long requestTimeoutSeconds) {
    }

    public record Retrieval(int topK) {
    }
}
