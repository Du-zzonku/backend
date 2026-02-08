package com.test.dosa_backend.controller;

import com.test.dosa_backend.dto.ChatDtos;
import com.test.dosa_backend.service.ChatService;
import com.test.dosa_backend.util.ImageInputs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/chat")
@Tag(name = "Chat", description = "Stateless chat message API")
public class ChatController {

    private final ChatService chatService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/messages")
    @Operation(summary = "Send a chat message")
    public ChatDtos.MessageResponse message(@Valid @RequestBody ChatDtos.MessageRequest req) {
        Map<String, Object> effectiveMetadata = mergeRequestMetadata(req);
        var res = chatService.userMessage(
                req.message(),
                req.documentIds(),
                req.imageUrls(),
                effectiveMetadata,
                req.history()
        );
        return toMessageResponse(res);
    }

    @PostMapping(
            path = "/messages:multipart",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Send a chat message with uploaded image file(s) and optional metadata/history")
    public ChatDtos.MessageResponse messageMultipart(
            @Parameter(description = "User message text") @RequestPart("message") String message,
            @Parameter(description = "Optional document UUIDs (repeat the field)") @RequestPart(value = "documentIds", required = false) List<UUID> documentIds,
            @Parameter(description = "Optional metadata as a JSON object string") @RequestPart(value = "extraMetadata", required = false) String extraMetadataJson,
            @Parameter(description = "Optional history as JSON array string") @RequestPart(value = "history", required = false) String historyJson,
            @Parameter(
                    description = "Uploaded image file(s)",
                    content = @Content(array = @ArraySchema(schema = @Schema(type = "string", format = "binary")))
            ) @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required.");
        }

        Map<String, Object> extraMetadata = parseExtraMetadata(extraMetadataJson);
        List<ChatDtos.HistoryMessage> history = parseHistory(historyJson);
        List<String> imageInputs = ImageInputs.filesToDataUrls(images);

        var res = chatService.userMessage(message, documentIds, imageInputs, extraMetadata, history);
        return toMessageResponse(res);
    }

    private ChatDtos.MessageResponse toMessageResponse(ChatService.ChatTurnResult res) {
        ChatDtos.AppliedSystemPrompt dtoPrompt = null;
        if (res != null && res.appliedSystemPrompt() != null) {
            dtoPrompt = new ChatDtos.AppliedSystemPrompt(
                    res.appliedSystemPrompt().rootSystemPrompt(),
                    res.appliedSystemPrompt().modelId(),
                    res.appliedSystemPrompt().modelSystemPromptApplied(),
                    res.appliedSystemPrompt().modelSystemPrompt()
            );
        }
        return new ChatDtos.MessageResponse(res.answer(), res.citations(), dtoPrompt);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseExtraMetadata(String extraMetadataJson) {
        if (extraMetadataJson == null || extraMetadataJson.isBlank()) return null;
        try {
            Object parsed = mapper.readValue(extraMetadataJson, Object.class);
            if (parsed instanceof Map<?, ?> m) {
                return (Map<String, Object>) m;
            }
            throw new IllegalArgumentException("extraMetadata must be a JSON object.");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("extraMetadata must be valid JSON.", e);
        }
    }

    private List<ChatDtos.HistoryMessage> parseHistory(String historyJson) {
        if (historyJson == null || historyJson.isBlank()) return null;
        try {
            Object parsed = mapper.readValue(historyJson, Object.class);
            if (!(parsed instanceof List<?> list)) {
                throw new IllegalArgumentException("history must be a JSON array.");
            }
            return mapper.convertValue(list, new TypeReference<List<ChatDtos.HistoryMessage>>() {});
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("history must be valid JSON.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeRequestMetadata(ChatDtos.MessageRequest req) {
        if (req == null) return null;
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>();

        if (req.extraMetadata() != null && !req.extraMetadata().isEmpty()) {
            merged.putAll(req.extraMetadata());
        }
        if (req.model() != null && !req.model().isEmpty()) {
            merged.put("model", req.model());
        }
        if (req.parts() != null) {
            Object normalizedParts = req.parts();
            if (normalizedParts instanceof Map<?, ?> map) {
                normalizedParts = List.of((Map<String, Object>) map);
            } else if (!(normalizedParts instanceof List<?>)) {
                throw new IllegalArgumentException("parts must be a JSON object or array.");
            }
            merged.put("parts", normalizedParts);
        }

        return merged.isEmpty() ? null : merged;
    }
}
