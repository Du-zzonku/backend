package com.test.dosa_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.test.dosa_backend.domain.DocumentChunk;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    List<DocumentChunk> findByDocument_IdOrderByChunkIndexAsc(UUID documentId);

    @Modifying
    @Transactional
    @Query("delete from DocumentChunk c where c.document.id = :documentId")
    void deleteAllByDocumentId(@Param("documentId") UUID documentId);
}
