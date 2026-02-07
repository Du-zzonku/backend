package com.test.dosa_backend.controller;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.test.dosa_backend.domain.Document;
import com.test.dosa_backend.domain.IngestJob;
import com.test.dosa_backend.dto.DocumentDtos;
import com.test.dosa_backend.service.DocumentService;
import com.test.dosa_backend.service.IngestService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v1/documents")
@Tag(name = "Documents", description = "Document upload and ingestion management")
public class DocumentController {

    private final DocumentService documentService;
    private final IngestService ingestService;

    public DocumentController(DocumentService documentService, IngestService ingestService) {
        this.documentService = documentService;
        this.ingestService = ingestService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentDtos.DocumentResponse upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) String title
    ) throws IOException {
        Document doc = documentService.uploadPdf(title, file);
        return new DocumentDtos.DocumentResponse(doc.getId(), doc.getTitle(), doc.getStatus(), doc.getCreatedAt(), doc.getUpdatedAt());
    }

    @GetMapping("/{id}")
    public DocumentDtos.DocumentResponse get(@PathVariable UUID id) {
        Document doc = documentService.get(id).orElseThrow(() -> new IllegalArgumentException("document not found"));
        return new DocumentDtos.DocumentResponse(doc.getId(), doc.getTitle(), doc.getStatus(), doc.getCreatedAt(), doc.getUpdatedAt());
    }

    @PostMapping("/{id}/ingest")
    public DocumentDtos.IngestJobResponse ingest(@PathVariable UUID id) {
        IngestJob job = ingestService.createJob(id);
        ingestService.runJobAsync(job.getId());
        return new DocumentDtos.IngestJobResponse(job.getId(), id, job.getStatus().name(), job.getCreatedAt(), job.getStartedAt(), job.getFinishedAt(), job.getChunkCount(), job.getEmbeddingModel(), job.getErrorMessage());
    }

    @GetMapping("/ingest-jobs/{jobId}")
    public DocumentDtos.IngestJobResponse ingestJob(@PathVariable UUID jobId) {
        IngestJob job = ingestService.getJob(jobId);
        return new DocumentDtos.IngestJobResponse(job.getId(), job.getDocument().getId(), job.getStatus().name(), job.getCreatedAt(), job.getStartedAt(), job.getFinishedAt(), job.getChunkCount(), job.getEmbeddingModel(), job.getErrorMessage());
    }
}
