package com.test.dosa_backend.dto;

import java.time.Instant;
import java.util.UUID;

import com.test.dosa_backend.domain.DocumentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public class DocumentDtos {
    public record DocumentResponse(
            @Schema(description = "Document ID (UUID)")
            UUID documentId,
            String title,
            DocumentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record IngestJobResponse(
            @Schema(description = "Ingest job ID (UUID)")
            UUID jobId,
            @Schema(description = "Target document ID (UUID)")
            UUID documentId,
            String status,
            Instant createdAt,
            Instant startedAt,
            Instant finishedAt,
            Integer chunkCount,
            String embeddingModel,
            String errorMessage
    ) {}
}
