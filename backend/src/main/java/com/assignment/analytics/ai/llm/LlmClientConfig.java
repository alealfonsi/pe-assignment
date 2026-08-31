package com.assignment.analytics.ai.llm;

import com.assignment.analytics.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Selects the LLM backend at startup.
 *
 * AUTO      -> Anthropic when ANTHROPIC_API_KEY is set, otherwise the stub
 * ANTHROPIC -> require the real backend; fail fast without a key
 * STUB      -> always the deterministic stub
 */
@Configuration
public class LlmClientConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmClientConfig.class);

    @Bean
    public LlmClient llmClient(AiProperties properties, Environment environment) {
        String apiKey = environment.getProperty("ANTHROPIC_API_KEY", "");
        boolean hasKey = !apiKey.isBlank();
        return switch (properties.mode()) {
            case STUB -> logged(new StubLlmClient(), "app.ai.mode=STUB");
            case ANTHROPIC -> {
                if (!hasKey) {
                    throw new IllegalStateException(
                            "app.ai.mode=ANTHROPIC requires the ANTHROPIC_API_KEY environment variable");
                }
                yield logged(new AnthropicLlmClient(apiKey, properties.anthropic()), "app.ai.mode=ANTHROPIC");
            }
            case AUTO -> hasKey
                    ? logged(new AnthropicLlmClient(apiKey, properties.anthropic()),
                    "app.ai.mode=AUTO with ANTHROPIC_API_KEY present")
                    : logged(new StubLlmClient(), "app.ai.mode=AUTO without ANTHROPIC_API_KEY");
        };
    }

    private LlmClient logged(LlmClient client, String reason) {
        log.info("LLM backend: {} (model '{}') - {}", client.mode(), client.modelName(), reason);
        return client;
    }
}
