package com.test.dosa_backend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.test.dosa_backend.domain.Document;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
}
