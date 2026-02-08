package com.test.dosa_backend.service;

import com.test.dosa_backend.config.ChatPromptProperties;
import com.test.dosa_backend.dto.ChatDtos;
import com.test.dosa_backend.openai.OpenAiClient;
import com.test.dosa_backend.util.ImageInputs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final String DEFAULT_CHAT_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_ROOT_SYSTEM_PROMPT = "당신은 과학/공학 학습용 3D 뷰어 서비스의 AI 튜터입니다.";
    private static final int MAX_HISTORY_MESSAGES = 30;
    private static final int DEFAULT_MAX_OUTPUT_TOKENS = 700;

    private final RagService ragService;
    private final OpenAiClient openAiClient;
    private final ChatPromptProperties promptProperties;
    private final ObjectMapper mapper = new ObjectMapper();

    private final String chatModel;

    public ChatService(
            RagService ragService,
            OpenAiClient openAiClient,
            ChatPromptProperties promptProperties,
            @Value("${openai.chat-model}") String chatModel
    ) {
        this.ragService = ragService;
        this.openAiClient = openAiClient;
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
            List<UUID> documentIds,
            List<String> imageUrls,
            Map<String, Object> extraMetadata,
            List<ChatDtos.HistoryMessage> history
    ) {
        if (userText == null || userText.isBlank()) {
            throw new IllegalArgumentException("message is required.");
        }

        List<String> normalizedImages = ImageInputs.normalizeImageInputs(imageUrls);
        boolean useRag = (documentIds != null && !documentIds.isEmpty());
        RagService.RagResult rag = useRag ? ragService.retrieve(userText, 6, documentIds)
                : new RagService.RagResult("", List.of());

        String effectiveModelId = findModelIdFromMetadata(extraMetadata);
        String prompt = buildUserPrompt(userText, rag.contextText(), extraMetadata, useRag, effectiveModelId);
        String instructions = buildSystemInstructions(useRag, effectiveModelId);
        List<OpenAiClient.ChatInputMessage> conversation = buildConversation(history, prompt, normalizedImages);

        log.info("Using chat model='{}', modelId='{}', historyCount={}",
                chatModel,
                effectiveModelId == null ? "" : effectiveModelId,
                conversation.size() - 1);

        String assistantText = openAiClient.generateResponse(chatModel, instructions, conversation, DEFAULT_MAX_OUTPUT_TOKENS);
        return new ChatTurnResult(assistantText, rag.citations());
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

    private String buildSystemInstructions(boolean useRag, String modelId) {
        String root = promptProperties.getRootSystemPrompt();
        if (root == null || root.isBlank()) {
            root = DEFAULT_ROOT_SYSTEM_PROMPT;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(root.trim());

        String modelPrompt = promptProperties.findModelSystemPrompt(modelId);
        if (modelPrompt != null && !modelPrompt.isBlank()) {
            sb.append("\n\n[Model System Prompt: ").append(modelId).append("]\n");
            sb.append(modelPrompt.trim());
        }

        sb.append("\n\n[Global Answer Rules]\n");
        sb.append("- 한국어로 답변\n");
        sb.append("- 학습자 수준에 맞게 정확하고 이해하기 쉽게 설명\n");
        sb.append("- 원리 -> 근거 -> 결론 순서로 설명\n");

        if (useRag) {
            sb.append("- 제공된 Sources 범위 안에서만 판단\n");
            sb.append("- 문장 끝에 [S1], [S2]처럼 인용 태그를 붙이기\n");
            sb.append("- Sources에 근거가 없으면 확답하지 말고 부족한 자료를 명시\n");
        } else {
            sb.append("- 일반 공학 지식을 사용해 답변 가능\n");
            sb.append("- 불확실하면 단정하지 말고 추가 자료 필요성을 명시\n");
        }
        return sb.toString();
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

    public record ChatTurnResult(String answer, List<RagService.Citation> citations) {}
}
