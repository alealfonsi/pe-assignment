package com.assignment.analytics.ai.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredTextBlock;
import com.assignment.analytics.ai.ActivityDigest;
import com.assignment.analytics.ai.model.RiskAnalysisResult;
import com.assignment.analytics.ai.model.TriagePlan;
import com.assignment.analytics.config.AiProperties;
import com.assignment.analytics.domain.LlmMode;
import com.assignment.analytics.rag.RetrievedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * Real LLM backend using the official Anthropic Java SDK.
 *
 * Both calls use structured outputs: the SDK derives a JSON schema from the
 * target record ({@link TriagePlan} / {@link RiskAnalysisResult}) and the API
 * constrains generation to that schema, so parsing is type-safe and there is
 * no free-text JSON extraction. The model (Claude Opus 5) has adaptive
 * thinking enabled by default; no thinking configuration is sent.
 */
public class AnthropicLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmClient.class);

    /** The triage output is tiny; a smaller cap keeps the call fast and bounded. */
    private static final long TRIAGE_MAX_TOKENS = 2048;

    private final AnthropicClient client;
    private final String model;
    private final long analysisMaxTokens;

    public AnthropicLlmClient(String apiKey, AiProperties.Anthropic config) {
        this(AnthropicOkHttpClient.builder()
                        .apiKey(apiKey)
                        .timeout(Duration.ofSeconds(config.requestTimeoutSeconds()))
                        .build(),
                config);
    }

    AnthropicLlmClient(AnthropicClient client, AiProperties.Anthropic config) {
        this.client = client;
        this.model = config.model();
        this.analysisMaxTokens = config.maxOutputTokens();
    }

    @Override
    public LlmMode mode() {
        return LlmMode.ANTHROPIC;
    }

    @Override
    public String modelName() {
        return model;
    }

    @Override
    public LlmInvocation<TriagePlan> triage(ActivityDigest digest, String systemPrompt, String userPrompt) {
        return call(TriagePlan.class, systemPrompt, userPrompt, TRIAGE_MAX_TOKENS, "triage");
    }

    @Override
    public LlmInvocation<RiskAnalysisResult> analyze(ActivityDigest digest, List<RetrievedChunk> chunks,
                                                     String systemPrompt, String userPrompt) {
        return call(RiskAnalysisResult.class, systemPrompt, userPrompt, analysisMaxTokens, "analysis");
    }

    private <T> LlmInvocation<T> call(Class<T> outputType, String systemPrompt, String userPrompt,
                                      long maxTokens, String stage) {
        StructuredMessageCreateParams<T> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .system(systemPrompt)
                .outputConfig(outputType)
                .addUserMessage(userPrompt)
                .build();
        try {
            StructuredMessage<T> response = client.messages().create(params);
            checkStopReason(response, stage);
            T output = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(StructuredTextBlock::text)
                    .findFirst()
                    .orElseThrow(() -> new LlmException("LLM returned no structured output for " + stage));
            log.info("Anthropic {} call ok: model={}, in={} tokens, out={} tokens",
                    stage, model, response.usage().inputTokens(), response.usage().outputTokens());
            return new LlmInvocation<>(output, response.usage().inputTokens(), response.usage().outputTokens());
        } catch (AnthropicServiceException e) {
            throw new LlmException("Anthropic API error during " + stage + " (HTTP " + e.statusCode() + ")", e);
        } catch (AnthropicIoException e) {
            throw new LlmException("Could not reach the Anthropic API during " + stage, e);
        }
    }

    private void checkStopReason(StructuredMessage<?> response, String stage) {
        response.stopReason().ifPresent(stopReason -> {
            if (StopReason.REFUSAL.equals(stopReason)) {
                String explanation = response.stopDetails()
                        .flatMap(details -> details.explanation())
                        .orElse("no explanation provided");
                throw new LlmException("The model declined the " + stage + " request: " + explanation);
            }
            if (StopReason.MAX_TOKENS.equals(stopReason)) {
                throw new LlmException("The " + stage + " response was truncated (max_tokens reached); "
                        + "increase app.ai.anthropic.max-output-tokens");
            }
        });
    }
}
