package com.test.dosa_backend.openai;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final WebClient webClient;
    private final Duration timeout;
    private final ObjectMapper mapper;

    private final String embeddingModel;
    private final int embeddingDimensions;
    private final int responsesMaxOutputTokens;
    private final int responsesRetryMaxOutputTokens;
    private final String responsesReasoningEffort;
    private final int chatMaxCompletionTokens;

    private static final String PARTIAL_WARNING =
            "\n\n(답변이 길어 일부가 생략되었습니다. 질문을 나눠서 요청하면 더 자세히 답할 수 있습니다.)";

    public OpenAiClient(
            WebClient openaiWebClient,
            ObjectMapper objectMapper,
            Duration openaiTimeout,
            @Value("${openai.embedding-model}") String embeddingModel,
            @Value("${openai.embedding-dimensions}") int embeddingDimensions,
            @Value("${openai.responses.max-output-tokens}") int responsesMaxOutputTokens,
            @Value("${openai.responses.retry-max-output-tokens}") int responsesRetryMaxOutputTokens,
            @Value("${openai.responses.reasoning-effort}") String responsesReasoningEffort,
            @Value("${openai.chat.max-completion-tokens}") int chatMaxCompletionTokens
    ) {
        this.webClient = openaiWebClient;
        this.mapper = objectMapper;
        this.timeout = openaiTimeout;
        this.embeddingModel = embeddingModel;
        this.embeddingDimensions = embeddingDimensions;
        this.responsesMaxOutputTokens = responsesMaxOutputTokens;
        this.responsesRetryMaxOutputTokens = responsesRetryMaxOutputTokens;
        this.responsesReasoningEffort = responsesReasoningEffort;
        this.chatMaxCompletionTokens = chatMaxCompletionTokens;
    }

    public String embeddingModel() {
        return embeddingModel;
    }

    /**
     * Calls POST /v1/embeddings
     */
    public List<float[]> embedTexts(List<String> inputs) {
        if (inputs == null || inputs.isEmpty()) return List.of();

        ObjectNode body = mapper.createObjectNode();
        body.put("model", embeddingModel);
        ArrayNode inputArr = body.putArray("input");
        for (String s : inputs) {
            inputArr.add(s);
        }
        if (embeddingDimensions > 0) {
            body.put("dimensions", embeddingDimensions);
        }

        JsonNode resp = postJson("/embeddings", body, "Embeddings API call failed");

        if (resp == null || !resp.has("data")) {
            throw new OpenAiException("Embeddings API returned empty response");
        }

        List<float[]> out = new ArrayList<>();
        for (JsonNode item : resp.get("data")) {
            JsonNode emb = item.get("embedding");
            float[] vec = new float[emb.size()];
            for (int i = 0; i < emb.size(); i++) {
                vec[i] = (float) emb.get(i).asDouble();
            }
            out.add(vec);
        }
        return out;
    }

    /**
     * Calls POST /v1/responses
     */
    public String generateResponse(String model, String instructions, String userText, List<String> imageUrls, int maxOutputTokens) {
        return generateResponse(model, instructions,
                List.of(new ChatInputMessage("user", userText, imageUrls)),
                maxOutputTokens);
    }

    public String generateResponse(String model, String instructions, List<ChatInputMessage> messages, int maxOutputTokens) {
        List<ChatInputMessage> normalizedMessages = normalizeMessages(messages);
        if (normalizedMessages.isEmpty()) {
            throw new OpenAiException("At least one chat message is required.");
        }
        try {
            return generateViaResponses(model, instructions, normalizedMessages, maxOutputTokens);
        } catch (OpenAiException e) {
            log.warn("Responses API failed; attempting chat completions fallback.", e);
            try {
                return generateViaChatCompletions(model, instructions, normalizedMessages, maxOutputTokens);
            } catch (OpenAiException e2) {
                throw new OpenAiException("Responses API failed and chat completions fallback also failed: " + e2.getMessage(), e2);
            }
        }
    }

    private String generateViaResponses(String model, String instructions, List<ChatInputMessage> messages, int maxOutputTokens) {
        ResponseOutcome first = callResponses(model, instructions, messages, maxOutputTokens, responsesReasoningEffort);
        if (first.hasText() && "completed".equals(first.status)) {
            return first.text;
        }

        boolean shouldRetry = "max_output_tokens".equals(first.incompleteReason);
        if (shouldRetry) {
            log.warn("Responses API incomplete (status={}, reason={}); retrying with higher token budget.",
                    first.status, first.incompleteReason);
            ResponseOutcome second = callResponses(model, instructions, messages, responsesRetryMaxOutputTokens, responsesReasoningEffort);
            if (second.hasText() && "completed".equals(second.status)) {
                return second.text;
            }
            if (second.hasText()) {
                log.warn("Responses API retry still incomplete (status={}, reason={}); returning partial response.",
                        second.status, second.incompleteReason);
                return second.text + PARTIAL_WARNING;
            }
            log.warn("Responses API retry produced no text (status={}, reason={}).", second.status, second.incompleteReason);
        } else if (first.hasText()) {
            log.warn("Responses API incomplete (status={}, reason={}); returning available text without retry.",
                    first.status, first.incompleteReason);
            return first.text;
        }

        throw new OpenAiException("Responses API returned empty or incomplete response (status=" + first.status + ", reason=" + first.incompleteReason + ")");
    }

    private ResponseOutcome callResponses(
            String model,
            String instructions,
            List<ChatInputMessage> messages,
            int maxOutputTokens,
            String reasoningEffort
    ) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        if (instructions != null && !instructions.isBlank()) {
            body.put("instructions", instructions);
        }
        int effectiveMaxTokens = resolveEffectiveTokenBudget(maxOutputTokens, responsesMaxOutputTokens);
        if (effectiveMaxTokens > 0) {
            body.put("max_output_tokens", effectiveMaxTokens);
        }
        if (reasoningEffort != null && !reasoningEffort.isBlank()) {
            ObjectNode reasoning = body.putObject("reasoning");
            reasoning.put("effort", reasoningEffort);
        }

        ArrayNode input = body.putArray("input");
        for (ChatInputMessage msg : messages) {
            ObjectNode node = input.addObject();
            node.put("role", normalizeRole(msg.role()));
            ArrayNode content = node.putArray("content");
            content.addObject().put("type", "input_text").put("text", msg.text());
            if (msg.imageUrls() != null && "user".equals(normalizeRole(msg.role()))) {
                for (String url : msg.imageUrls()) {
                    if (url == null || url.isBlank()) continue;
                    ObjectNode img = content.addObject();
                    img.put("type", "input_image");
                    img.put("image_url", url);
                }
            }
        }

        JsonNode resp = postJson("/responses", body, "Responses API call failed");

        if (resp == null) {
            throw new OpenAiException("Responses API returned empty response");
        }
        String status = resp.path("status").asText("");
        String incompleteReason = resp.path("incomplete_details").path("reason").asText("");

        // 1) SDK 편의 필드가 실제 JSON에 있을 수도/없을 수도 있으니, 있으면 사용
        if (resp.hasNonNull("output_text") && resp.get("output_text").isTextual()) {
            String t = resp.get("output_text").asText("");
            String out = t == null ? "" : t.trim();
            log.info("Responses API status={}, reason={}, output_length={}", status, incompleteReason, out.length());
            return new ResponseOutcome(out, status, incompleteReason);
        }

        // 2) 가장 안전한 방식: output 전체를 훑어서 output_text 조각을 전부 이어붙이기
        StringBuilder sb = new StringBuilder();
        if (resp.has("output") && resp.get("output").isArray()) {
            for (JsonNode item : resp.get("output")) {
                JsonNode contentArr = item.path("content");
                if (!contentArr.isArray()) continue;

                for (JsonNode part : contentArr) {
                    String type = part.path("type").asText("");

                    // 공식 예시: { "type":"output_text", "text":"..." }
                    if ("output_text".equals(type)) {
                        JsonNode textNode = part.get("text");
                        if (textNode == null || textNode.isNull()) continue;

                        if (textNode.isTextual()) {
                            sb.append(textNode.asText());
                        } else if (textNode.isObject()) {
                            // 혹시 text가 객체로 오는 변형 케이스 방어: { "value": "..." }
                            String v = textNode.path("value").asText("");
                            if (!v.isBlank()) sb.append(v);
                        }
                    }
                }
            }
        }

        String out = sb.toString().trim();
        log.info("Responses API status={}, reason={}, output_length={}", status, incompleteReason, out.length());
        return new ResponseOutcome(out, status, incompleteReason);
    }

    private String generateViaChatCompletions(String model, String instructions, List<ChatInputMessage> chatMessages, int maxOutputTokens) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        int totalInputLength = chatMessages.stream().mapToInt(m -> m.text() == null ? 0 : m.text().length()).sum();
        log.info("Chat Completions request - Model: '{}', Instructions: {}, Message count: {}, Total text length: {}",
                model, (instructions != null && !instructions.isBlank()), chatMessages.size(), totalInputLength);
        // GPT-5 family uses max_completion_tokens (not max_tokens)
        int effectiveMaxCompletion = resolveEffectiveTokenBudget(maxOutputTokens, chatMaxCompletionTokens);
        if (effectiveMaxCompletion > 0) {
            body.put("max_completion_tokens", effectiveMaxCompletion);
        }

        ArrayNode messagesArray = body.putArray("messages");
        if (instructions != null && !instructions.isBlank()) {
            messagesArray.addObject()
                    .put("role", "system")
                    .put("content", instructions);
        }

        for (ChatInputMessage msg : chatMessages) {
            String role = normalizeRole(msg.role());
            ObjectNode one = messagesArray.addObject();
            one.put("role", role);
            List<String> imageUrls = msg.imageUrls();
            String text = msg.text() == null ? "" : msg.text();
            if ("user".equals(role) && imageUrls != null && imageUrls.stream().anyMatch(u -> u != null && !u.isBlank())) {
                ArrayNode content = one.putArray("content");
                content.addObject().put("type", "text").put("text", text);
                for (String url : imageUrls) {
                    if (url == null || url.isBlank()) continue;
                    ObjectNode img = content.addObject();
                    img.put("type", "image_url");
                    ObjectNode imageUrl = img.putObject("image_url");
                    imageUrl.put("url", url);
                }
            } else {
                one.put("content", text);
            }
        }

        JsonNode resp = postJson("/chat/completions", body, "Chat Completions API call failed");
        if (resp == null) {
            throw new OpenAiException("Chat Completions API returned empty response");
        }

        JsonNode choices = resp.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            JsonNode msg = choices.get(0).path("message");
            String content = msg.path("content").asText("");
            if (!content.isBlank()) {
                return content;
            }
        }
        throw new OpenAiException("Chat Completions returned empty content");
    }

    private List<ChatInputMessage> normalizeMessages(List<ChatInputMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<ChatInputMessage> out = new ArrayList<>();
        for (ChatInputMessage m : messages) {
            if (m == null || m.text() == null || m.text().isBlank()) continue;
            String role = normalizeRole(m.role());
            List<String> imageUrls = (m.imageUrls() == null) ? List.of() : m.imageUrls();
            out.add(new ChatInputMessage(role, m.text().trim(), imageUrls));
        }
        return out;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) return "user";
        String r = role.trim().toLowerCase(java.util.Locale.ROOT);
        if ("human".equals(r)) return "user";
        if ("ai".equals(r) || "bot".equals(r)) return "assistant";
        if ("assistant".equals(r) || "user".equals(r) || "system".equals(r)) return r;
        return "user";
    }

    private JsonNode postJson(String path, ObjectNode body, String errorPrefix) {
        String bodyString;
        try {
            bodyString = mapper.writeValueAsString(body);
            log.info("OpenAI request path={} payload_bytes={} has_model={}",
                    path, bodyString.length(), body.has("model"));
            if (log.isDebugEnabled()) {
                log.debug("OpenAI request body path={}: {}", path, bodyString);
            }
        } catch (Exception e) {
            log.error("Could not serialize body for logging: {}", e.getMessage(), e);
            throw new OpenAiException("Failed to serialize request body", e);
        }

        long startNanos = System.nanoTime();
        String raw = webClient.post()
                .uri(path)
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue(bodyString)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(bodyText -> {
                                    log.error("OpenAI API Error Response - Status: {}, Body: {}",
                                            response.statusCode().value(), bodyText);
                                    return Mono.error(
                                            new OpenAiException(errorPrefix + " (HTTP " + response.statusCode().value() + "): " + bodyText)
                                    );
                                })
                )
                .bodyToMono(String.class)
                .timeout(timeout)
                .onErrorResume(ex -> {
                    if (ex instanceof OpenAiException) return Mono.error(ex);
                    return Mono.error(new OpenAiException(errorPrefix, ex));
                })
                .block();

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        if (raw == null || raw.isBlank()) {
            log.warn("OpenAI response empty path={} latency_ms={}", path, elapsedMs);
            return null;
        }
        log.info("OpenAI response path={} latency_ms={} response_bytes={}", path, elapsedMs, raw.length());
        if (log.isDebugEnabled()) {
            log.debug("OpenAI raw response path={}: {}", path, raw);
        }

        try {
            return mapper.readTree(raw);
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response JSON. raw={}", raw, e);
            throw new OpenAiException("Failed to parse OpenAI response JSON", e);
        }
    }

    private int resolveEffectiveTokenBudget(int requestTokenBudget, int fallbackTokenBudget) {
        if (requestTokenBudget > 0) {
            return requestTokenBudget;
        }
        return Math.max(fallbackTokenBudget, 0);
    }

    private record ResponseOutcome(String text, String status, String incompleteReason) {
        boolean hasText() {
            return text != null && !text.isBlank();
        }
    }

    public record ChatInputMessage(String role, String text, List<String> imageUrls) {}
}
