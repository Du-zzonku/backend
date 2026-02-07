package com.test.dosa_backend.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingest_jobs")
public class IngestJob {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngestJobStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @Column(name = "embedding_model")
    private String embeddingModel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IngestJob() {}

    public IngestJob(UUID id, Document document, IngestJobStatus status, Instant createdAt) {
        this.id = id;
        this.document = document;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public Document getDocument() { return document; }
    public IngestJobStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public String getErrorMessage() { return errorMessage; }
    public Integer getChunkCount() { return chunkCount; }
    public String getEmbeddingModel() { return embeddingModel; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(IngestJobStatus status) { this.status = status; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
}
