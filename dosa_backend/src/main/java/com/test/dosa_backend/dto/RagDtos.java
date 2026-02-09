package com.test.dosa_backend.dto;

import java.util.List;
import java.util.UUID;

import com.test.dosa_backend.service.RagService;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class RagDtos {

    public record SearchRequest(
            @NotBlank
            @Schema(description = "Natural-language query text")
            String query,
            @Schema(description = "Number of top similar chunks to return (default: 5)")
            Integer topK,
            @ArraySchema(
                    schema = @Schema(format = "uuid"),
                    arraySchema = @Schema(description = "Optional document filters. If omitted or empty, search all ingested documents.")
            )
            List<UUID> documentIds
    ) {}

    public record SearchResponse(String contextText, List<RagService.Citation> citations) {}
}
