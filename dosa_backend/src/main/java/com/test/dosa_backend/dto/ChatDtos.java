package com.test.dosa_backend.dto;

import com.test.dosa_backend.service.RagService;
import com.fasterxml.jackson.annotation.JsonAlias;
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
            List<UUID> documentIds,
            List<String> imageUrls,
            Map<String, Object> extraMetadata,
            @JsonAlias({"conversationHistory"}) List<@Valid HistoryMessage> history,
            @JsonAlias({"modelMetadata"}) Map<String, Object> model,
            @JsonAlias({"part", "selectedParts"}) Object parts
    ) {}

    public record MessageResponse(
            String answer,
            List<RagService.Citation> citations
    ) {}
}
