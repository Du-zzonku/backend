package com.test.dosa_backend.dto;

import java.time.Instant;
import java.util.UUID;

import com.test.dosa_backend.domain.DocumentStatus;

public class DocumentDtos {
    public record DocumentResponse(UUID id, String title, DocumentStatus status, Instant createdAt, Instant updatedAt) {}
    public record IngestJobResponse(UUID id, UUID documentId, String status, Instant createdAt, Instant startedAt, Instant finishedAt, Integer chunkCount, String embeddingModel, String errorMessage) {}
}
