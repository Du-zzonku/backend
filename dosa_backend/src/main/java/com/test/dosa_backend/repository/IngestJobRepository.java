package com.test.dosa_backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.test.dosa_backend.domain.IngestJob;

public interface IngestJobRepository extends JpaRepository<IngestJob, UUID> {
    List<IngestJob> findByDocument_Id(UUID documentId);
}
