package com.test.dosa_backend.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.test.dosa_backend.config.ChatPromptProperties;
import com.test.dosa_backend.domain.Part;
import com.test.dosa_backend.dto.ChatDtos;
import com.test.dosa_backend.openai.OpenAiClient;
import com.test.dosa_backend.repository.PartRepository;
import com.test.dosa_backend.util.ImageInputs;

import tools.jackson.databind.ObjectMapper;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final String DEFAULT_CHAT_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_ROOT_SYSTEM_PROMPT = "당신은 과학/공학 학습용 3D 뷰어 서비스의 AI 튜터입니다.";
    private static final int MAX_HISTORY_MESSAGES = 30;
    private static final int DEFAULT_MAX_OUTPUT_TOKENS = 700;
    private static final String KEY_MODEL = "model";
    private static final String KEY_PARTS = "parts";
    private static final Map<String, List<String>> MODEL_ID_ALIASES = Map.of(
            "v4_engine", List.of("v4_engine", "v4-engine", "v4 engine", "v4엔진", "v4 엔진"),
            "suspension", List.of("suspension", "서스펜션"),
            "robot_gripper", List.of("robot_gripper", "robot-gripper", "robot gripper", "그리퍼", "로봇그리퍼", "로봇 그리퍼"),
            "drone", List.of("drone", "드론"),
            "robot_arm", List.of("robot_arm", "robot-arm", "robot arm", "로봇암", "로봇 암")
    );

    private final RagService ragService;
    private final OpenAiClient openAiClient;
    private final PartRepository partRepository;
    private final ChatPromptProperties promptProperties;
    private final ObjectMapper mapper = new ObjectMapper();

    private final String chatModel;

    public ChatService(
            RagService ragService,
            OpenAiClient openAiClient,
            PartRepository partRepository,
            ChatPromptProperties promptProperties,
            @Value("${openai.chat-model}") String chatModel
    ) {
        this.ragService = ragService;
        this.openAiClient = openAiClient;
        this.partRepository = partRepository;
        this.promptProperties = promptProperties;
        log.info("ChatService constructor - Received chatModel value: '{}'", chatModel);
        if (chatModel == null || chatModel.isBlank()) {
            log.warn("openai.chat-model is blank; falling back to default model '{}'.", DEFAULT_CHAT_MODEL);
            this.chatModel = DEFAULT_CHAT_MODEL;
        } else {
            this.chatModel = chatModel;
        }
        log.info("ChatService initialized with chatModel: '{}'", this.chatModel);
    }

    @Transactional(readOnly = true)
    public ChatTurnResult userMessage(
            String userText,
            Boolean useRag,
            List<UUID> documentIds,
            List<String> imageUrls,
            Map<String, Object> extraMetadata,
            List<ChatDtos.HistoryMessage> history
    ) {
        if (userText == null || userText.isBlank()) {
            throw new IllegalArgumentException("message is required.");
        }

        List<String> normalizedImages = ImageInputs.normalizeImageInputs(imageUrls);
        boolean ragEnabled = Boolean.TRUE.equals(useRag);
        List<UUID> normalizedDocumentIds = normalizeDocumentIds(documentIds);
        String effectiveModelId = resolveEffectiveModelId(extraMetadata);
        Map<String, Object> metadataForPrompt = enrichMetadataForPrompt(extraMetadata, effectiveModelId);
        String ragQuery = buildRagQuery(userText, metadataForPrompt, history);

        RagService.RagResult rag = ragEnabled ? ragService.retrieve(ragQuery, 2, normalizedDocumentIds)
                : new RagService.RagResult("", List.of());

        AppliedSystemPrompt appliedSystemPrompt = resolveAppliedSystemPrompt(effectiveModelId);
        String prompt = buildUserPrompt(userText, rag.contextText(), metadataForPrompt, ragEnabled, effectiveModelId);
        String instructions = buildSystemInstructions(ragEnabled, appliedSystemPrompt);
        List<OpenAiClient.ChatInputMessage> conversation = buildConversation(history, prompt, normalizedImages);

        log.info("Using chat model='{}', modelId='{}', historyCount={}",
                chatModel,
                effectiveModelId == null ? "" : effectiveModelId,
                conversation.size() - 1);

        String assistantText = openAiClient.generateResponse(chatModel, instructions, conversation, DEFAULT_MAX_OUTPUT_TOKENS);
        return new ChatTurnResult(assistantText, rag.citations(), appliedSystemPrompt);
    }

    private List<UUID> normalizeDocumentIds(List<UUID> documentIds) {
        if (documentIds == null) {
            return null;
        }
        List<UUID> filtered = documentIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        return filtered.isEmpty() ? null : filtered;
    }

    private String buildRagQuery(
            String userText,
            Map<String, Object> metadataForPrompt,
            List<ChatDtos.HistoryMessage> history
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("User question:\n").append(userText == null ? "" : userText.trim()).append("\n");

        String recentHistory = recentHistoryForRag(history);
        if (!recentHistory.isBlank()) {
            sb.append("\nRecent conversation:\n").append(recentHistory).append("\n");
        }

        String metadataSummary = metadataSummaryForRag(metadataForPrompt);
        if (!metadataSummary.isBlank()) {
            sb.append("\nModel/parts context:\n").append(metadataSummary).append("\n");
        }

        String out = sb.toString().trim();
        if (out.length() > 4000) {
            return out.substring(0, 4000);
        }
        return out;
    }

    private String recentHistoryForRag(List<ChatDtos.HistoryMessage> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        int from = Math.max(0, history.size() - 6);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < history.size(); i++) {
            ChatDtos.HistoryMessage msg = history.get(i);
            if (msg == null || msg.content() == null || msg.content().isBlank()) {
                continue;
            }
            String role = normalizeRole(msg.role());
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            sb.append(role).append(": ").append(msg.content().trim()).append("\n");
        }
        return sb.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private String metadataSummaryForRag(Map<String, Object> metadataForPrompt) {
        if (metadataForPrompt == null || metadataForPrompt.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Object modelObj = metadataForPrompt.get(KEY_MODEL);
        if (modelObj instanceof Map<?, ?> model) {
            String modelId = firstNonBlank(model.get("modelId"), model.get("model_id"));
            String title = firstNonBlank(model.get("title"));
            if (modelId != null) sb.append("- modelId: ").append(modelId).append("\n");
            if (title != null) sb.append("- modelTitle: ").append(title).append("\n");
        }
        Object partsObj = metadataForPrompt.get(KEY_PARTS);
        if (partsObj instanceof List<?> parts && !parts.isEmpty()) {
            sb.append("- selectedParts:\n");
            for (Object partObj : parts) {
                if (!(partObj instanceof Map<?, ?> part)) continue;
                String partId = firstNonBlank(part.get("partId"), part.get("part_id"));
                if (partId == null) continue;
                String displayNameKo = firstNonBlank(part.get("displayNameKo"));
                String summary = firstNonBlank(part.get("summary"));
                sb.append("  - ").append(partId);
                if (displayNameKo != null) sb.append(" / ").append(displayNameKo);
                if (summary != null) sb.append(" / ").append(summary);
                sb.append("\n");
            }
        }
        return sb.toString().trim();
    }

    private List<OpenAiClient.ChatInputMessage> buildConversation(
            List<ChatDtos.HistoryMessage> history,
            String finalPrompt,
            List<String> normalizedImages
    ) {
        List<OpenAiClient.ChatInputMessage> out = new ArrayList<>();
        if (history != null && !history.isEmpty()) {
            List<OpenAiClient.ChatInputMessage> sanitized = new ArrayList<>();
            for (ChatDtos.HistoryMessage msg : history) {
                if (msg == null || msg.content() == null || msg.content().isBlank()) {
                    continue;
                }
                String role = normalizeRole(msg.role());
                if (!"user".equals(role) && !"assistant".equals(role)) {
                    continue;
                }
                sanitized.add(new OpenAiClient.ChatInputMessage(role, msg.content().trim(), List.of()));
            }
            int from = Math.max(0, sanitized.size() - MAX_HISTORY_MESSAGES);
            for (int i = from; i < sanitized.size(); i++) {
                out.add(sanitized.get(i));
            }
        }
        out.add(new OpenAiClient.ChatInputMessage("user", finalPrompt, normalizedImages));
        return out;
    }

    private String buildSystemInstructions(boolean useRag, AppliedSystemPrompt appliedSystemPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append(appliedSystemPrompt.rootSystemPrompt());

        if (appliedSystemPrompt.modelSystemPromptApplied()) {
            sb.append("\n\n[Model System Prompt: ").append(appliedSystemPrompt.modelId()).append("]\n");
            sb.append(appliedSystemPrompt.modelSystemPrompt());
        }

        sb.append("\n\n[Global Answer Rules]\n");
        sb.append("- 한국어로 답변\n");
        sb.append("- 최대한 요약하고 최소화해서 반드시 1000 토큰 이내로 답변\n");
        //sb.append("- 학습자 수준에 맞게 정확하고 이해하기 쉽게 설명\n");
        //sb.append("- 원리 -> 근거 -> 결론 순서로 설명\n");

        if (useRag) {
            sb.append("- 제공된 Sources 범위 안에서만 판단\n");
            sb.append("- 문장 끝에 [S1], [S2]처럼 인용 태그를 붙이기\n");
            //sb.append("- Sources에 근거가 없으면 확답하지 말고 부족한 자료를 명시\n");
        } else {
            sb.append("- 일반 공학 지식을 사용해 답변 가능\n");
            //sb.append("- 불확실하면 단정하지 말고 추가 자료 필요성을 명시\n");
        }
        return sb.toString();
    }

    private AppliedSystemPrompt resolveAppliedSystemPrompt(String modelId) {
        String root = normalizePromptText(promptProperties.getRootSystemPrompt(), DEFAULT_ROOT_SYSTEM_PROMPT);
        String resolvedModelId = (modelId == null || modelId.isBlank()) ? null : modelId.trim();
        String modelPrompt = normalizePromptText(promptProperties.findModelSystemPrompt(resolvedModelId), "");
        boolean applied = modelPrompt != null && !modelPrompt.isBlank();
        return new AppliedSystemPrompt(
                root,
                resolvedModelId,
                applied,
                applied ? modelPrompt : null
        );
    }

    private String resolveEffectiveModelId(Map<String, Object> metadata) {
        String fromMetadata = canonicalizeModelId(findModelIdFromMetadata(metadata));
        return (fromMetadata == null || fromMetadata.isBlank()) ? null : fromMetadata;
    }

    private Map<String, Object> enrichMetadataForPrompt(Map<String, Object> metadata, String modelId) {
        if (metadata == null || metadata.isEmpty()) {
            return metadata;
        }

        LinkedHashMap<String, Object> enriched = new LinkedHashMap<>(metadata);
        sanitizeModelMetadata(enriched, modelId);
        sanitizeAndEnrichParts(enriched, modelId);
        return enriched;
    }

    @SuppressWarnings("unchecked")
    private void sanitizeModelMetadata(Map<String, Object> metadata, String effectiveModelId) {
        if (metadata == null) return;
        Object modelObj = metadata.get(KEY_MODEL);
        if (!(modelObj instanceof Map<?, ?> modelMap)) {
            return;
        }

        String modelId = firstNonBlank(
                modelMap.get("modelId"),
                modelMap.get("model_id"),
                effectiveModelId
        );
        String title = textOrNull(modelMap.get("title"));

        LinkedHashMap<String, Object> sanitized = new LinkedHashMap<>();
        if (modelId != null) {
            sanitized.put("modelId", canonicalizeModelId(modelId));
        }
        if (title != null) {
            sanitized.put("title", title);
        }
        metadata.put(KEY_MODEL, sanitized);
    }

    private void sanitizeAndEnrichParts(Map<String, Object> metadata, String effectiveModelId) {
        if (metadata == null) return;
        List<Map<String, Object>> requestedParts = normalizeParts(metadata.get(KEY_PARTS));
        if (requestedParts.isEmpty()) {
            return;
        }

        List<String> requestedPartIds = requestedParts.stream()
                .map(this::extractPartId)
                .filter(id -> id != null && !id.isBlank())
                .toList();
        Map<String, Part> dbPartsById = loadPartsById(requestedPartIds, effectiveModelId);

        List<Map<String, Object>> sanitizedParts = new ArrayList<>();
        for (Map<String, Object> req : requestedParts) {
            String partId = extractPartId(req);
            if (partId == null || partId.isBlank()) {
                continue;
            }

            Part part = dbPartsById.get(partId);
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("partId", partId);

            String displayNameKo = (part != null)
                    ? textOrNull(part.getDisplayNameKo())
                    : textOrNull(req.get("displayNameKo"));
            String summary = (part != null)
                    ? textOrNull(part.getSummary())
                    : textOrNull(req.get("summary"));

            if (displayNameKo != null) out.put("displayNameKo", displayNameKo);
            if (summary != null) out.put("summary", summary);

            sanitizedParts.add(out);
        }
        metadata.put(KEY_PARTS, sanitizedParts);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeParts(Object rawParts) {
        if (rawParts == null) return List.of();
        if (rawParts instanceof Map<?, ?> singleMap) {
            return List.of((Map<String, Object>) singleMap);
        }
        if (!(rawParts instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                out.add((Map<String, Object>) map);
            }
        }
        return out;
    }

    private Map<String, Part> loadPartsById(List<String> partIds, String effectiveModelId) {
        Map<String, Part> out = new HashMap<>();
        if (partIds == null || partIds.isEmpty()) {
            return out;
        }

        Iterable<Part> parts = partRepository.findAllById(partIds);
        for (Part p : parts) {
            if (p == null || p.getPartId() == null) continue;
            if (effectiveModelId != null && p.getModel() != null && p.getModel().getModelId() != null) {
                if (!normalizeForMatch(effectiveModelId).equals(normalizeForMatch(p.getModel().getModelId()))) {
                    continue;
                }
            }
            out.put(p.getPartId(), p);
        }
        return out;
    }

    private String extractPartId(Map<String, Object> part) {
        if (part == null || part.isEmpty()) return null;
        String partId = firstNonBlank(part.get("partId"), part.get("part_id"));
        return (partId == null || partId.isBlank()) ? null : partId.trim();
    }

    private String firstNonBlank(Object... values) {
        if (values == null || values.length == 0) return null;
        for (Object value : values) {
            String text = textOrNull(value);
            if (text != null && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private String textOrNull(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private String canonicalizeModelId(String rawModelId) {
        if (rawModelId == null || rawModelId.isBlank()) {
            return null;
        }
        String normalizedRaw = normalizeForMatch(rawModelId);
        if (normalizedRaw.isBlank()) {
            return rawModelId.trim();
        }
        for (String modelId : knownModelIds()) {
            String normalizedModelId = normalizeForMatch(modelId);
            if (normalizedRaw.equals(normalizedModelId)) {
                return modelId;
            }
            for (String alias : aliasesForModel(modelId)) {
                if (normalizedRaw.equals(normalizeForMatch(alias))) {
                    return modelId;
                }
            }
        }
        return rawModelId.trim();
    }

    private List<String> knownModelIds() {
        Set<String> ids = new LinkedHashSet<>();
        ids.addAll(MODEL_ID_ALIASES.keySet());
        Map<String, String> configured = promptProperties.getModelSystemPrompts();
        if (configured != null && !configured.isEmpty()) {
            ids.addAll(configured.keySet());
        }
        List<String> out = new ArrayList<>(ids);
        out.sort(Comparator.naturalOrder());
        return out;
    }

    private List<String> aliasesForModel(String modelId) {
        List<String> aliases = new ArrayList<>();
        if (modelId != null && !modelId.isBlank()) {
            aliases.add(modelId);
            aliases.add(modelId.replace('_', '-'));
            aliases.add(modelId.replace('_', ' '));
            aliases.add(modelId.replace("_", ""));
            String normalizedKey = promptProperties.normalizeKey(modelId);
            aliases.addAll(MODEL_ID_ALIASES.getOrDefault(normalizedKey, List.of()));
        }
        return aliases;
    }

    private String normalizeForMatch(String value) {
        if (value == null) return "";
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9가-힣]", "");
    }

    private String normalizePromptText(String text, String fallback) {
        String effective = (text == null || text.isBlank()) ? fallback : text;
        if (effective == null) return "";
        String trimmed = effective.trim();
        if (trimmed.isBlank()) return "";
        if (containsHangul(trimmed)) return trimmed;

        String decoded = new String(trimmed.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8).trim();
        if (containsHangul(decoded)) {
            log.warn("Detected mojibake in chat prompt text; recovered UTF-8 prompt.");
            return decoded;
        }
        return trimmed;
    }

    private boolean containsHangul(String text) {
        if (text == null || text.isBlank()) return false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= '\uAC00' && ch <= '\uD7A3') {
                return true;
            }
        }
        return false;
    }

    private String buildUserPrompt(
            String userText,
            String ragContext,
            Map<String, Object> extraMetadata,
            boolean useRag,
            String modelId
    ) {
        StringBuilder sb = new StringBuilder();
        if (modelId != null && !modelId.isBlank()) {
            sb.append("# Active model\n").append(modelId).append("\n\n");
        }

        sb.append("# User question\n").append(userText).append("\n\n");

        if (extraMetadata != null && !extraMetadata.isEmpty()) {
            sb.append("# Extra metadata\n");
            try {
                sb.append(mapper.writeValueAsString(extraMetadata));
            } catch (Exception e) {
                sb.append(String.valueOf(extraMetadata));
            }
            sb.append("\n\n");
        }

        if (useRag) {
            if (ragContext != null && !ragContext.isBlank()) {
                sb.append("# Sources\n").append(ragContext).append("\n\n");
            } else {
                sb.append("# Sources\n(없음)\n\n");
            }
        }

        sb.append("# Answer requirements\n");
        sb.append("- 대화 기록(history)을 문맥으로 참고\n");
        if (useRag) {
            sb.append("- 근거 인용 태그 [S1], [S2]를 포함\n");
        } else {
            sb.append("- 확실하지 않은 내용은 추정으로 표시\n");
        }
        return sb.toString();
    }

    private String findModelIdFromMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        List<String> paths = promptProperties.getModelIdPaths();
        if (paths == null || paths.isEmpty()) {
            return null;
        }
        for (String path : paths) {
            Object value = findMetadataValueByPath(metadata, path);
            if (value == null) continue;
            String text = String.valueOf(value).trim();
            if (!text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private Object findMetadataValueByPath(Map<String, Object> metadata, String path) {
        if (metadata == null || metadata.isEmpty() || path == null || path.isBlank()) {
            return null;
        }

        Object current = metadata;
        String[] segments = path.split("\\.");
        for (String segment : segments) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = findMapValueByKey(map, segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private Object findMapValueByKey(Map<?, ?> map, String key) {
        if (map == null || map.isEmpty() || key == null || key.isBlank()) {
            return null;
        }
        String target = normalizeMetaKey(key);
        for (Map.Entry<?, ?> e : map.entrySet()) {
            String candidate = String.valueOf(e.getKey());
            if (normalizeMetaKey(candidate).equals(target)) {
                return e.getValue();
            }
        }
        return null;
    }

    private String normalizeMetaKey(String key) {
        if (key == null) return "";
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]", "");
    }

    private String normalizeRole(String rawRole) {
        if (rawRole == null) return "";
        String role = rawRole.trim().toLowerCase(Locale.ROOT);
        if ("human".equals(role)) return "user";
        if ("bot".equals(role) || "ai".equals(role)) return "assistant";
        return role;
    }

    public record ChatTurnResult(String answer, List<RagService.Citation> citations, AppliedSystemPrompt appliedSystemPrompt) {
        public ChatTurnResult(String answer, List<RagService.Citation> citations) {
            this(answer, citations, null);
        }
    }

    public record AppliedSystemPrompt(
            String rootSystemPrompt,
            String modelId,
            boolean modelSystemPromptApplied,
            String modelSystemPrompt
    ) {}
}
