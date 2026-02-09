package com.test.dosa_backend.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "content_text", nullable = false, columnDefinition = "text")
    private String contentText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta", nullable = false, columnDefinition = "jsonb")
    private String metaJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DocumentChunk() {}

    public DocumentChunk(UUID id, Document document, int chunkIndex, String contentText, String metaJson, Instant createdAt) {
        this.id = id;
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.contentText = contentText;
        this.metaJson = metaJson;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public Document getDocument() { return document; }
    public int getChunkIndex() { return chunkIndex; }
    public String getContentText() { return contentText; }
    public String getMetaJson() { return metaJson; }
    public Instant getCreatedAt() { return createdAt; }
}
