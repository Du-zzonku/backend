package com.test.dosa_backend.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "storage_uri", nullable = false)
    private String storageUri;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Document() {}

    public Document(UUID id, String title, String sourceType, String storageUri, DocumentStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.sourceType = sourceType;
        this.storageUri = storageUri;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getSourceType() { return sourceType; }
    public String getStorageUri() { return storageUri; }
    public DocumentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setTitle(String title) { this.title = title; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public void setStorageUri(String storageUri) { this.storageUri = storageUri; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
