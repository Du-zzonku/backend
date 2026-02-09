package com.test.dosa_backend.dto;

import com.test.dosa_backend.service.RagService;
import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatDtos {

    public record HistoryMessage(
            @NotBlank String role,
            @NotBlank String content
    ) {}

    public record MessageRequest(
            @NotBlank String message,
            @Schema(description = "Enable/disable RAG retrieval for this turn")
            Boolean useRag,
            @ArraySchema(schema = @Schema(format = "uuid"), arraySchema = @Schema(description = "Optional RAG document filter. If useRag=true and empty/omitted, search all ingested documents."))
            List<UUID> documentIds,
            List<String> imageUrls,
            Map<String, Object> extraMetadata,
            @JsonAlias({"conversationHistory"}) List<@Valid HistoryMessage> history,
            @JsonAlias({"modelMetadata"}) Map<String, Object> model,
            @JsonAlias({"part", "selectedParts"}) Object parts
    ) {}

    public record AppliedSystemPrompt(
            String rootSystemPrompt,
            String modelId,
            boolean modelSystemPromptApplied,
            String modelSystemPrompt
    ) {}

    public record MessageResponse(
            String answer,
            List<RagService.Citation> citations,
            AppliedSystemPrompt appliedSystemPrompt
    ) {}
}
