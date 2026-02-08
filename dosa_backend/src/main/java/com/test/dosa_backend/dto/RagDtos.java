package com.test.dosa_backend.dto;

import java.util.List;
import java.util.UUID;

import com.test.dosa_backend.service.RagService;

import jakarta.validation.constraints.NotBlank;

public class RagDtos {

    public record SearchRequest(@NotBlank String query, Integer topK, List<UUID> documentIds) {}

    public record SearchResponse(String contextText, List<RagService.Citation> citations) {}
}
