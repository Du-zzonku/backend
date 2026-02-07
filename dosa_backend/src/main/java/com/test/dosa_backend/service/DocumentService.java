package com.test.dosa_backend.service;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.test.dosa_backend.domain.Document;
import com.test.dosa_backend.domain.DocumentStatus;
import com.test.dosa_backend.repository.DocumentRepository;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final StorageService storageService;

    public DocumentService(DocumentRepository documentRepository, StorageService storageService) {
        this.documentRepository = documentRepository;
        this.storageService = storageService;
    }

    @Transactional
    public Document uploadPdf(String title, MultipartFile pdfFile) throws IOException {
        UUID id = UUID.randomUUID();
        StorageService.StoredFile stored = storageService.savePdf(id, pdfFile);
        Instant now = Instant.now();
        Document doc = new Document(
                id,
                (title == null || title.isBlank()) ? stored.filename() : title,
                "pdf",
                stored.path(),
                DocumentStatus.UPLOADED,
                now,
                now
        );
        return documentRepository.save(doc);
    }

    public Optional<Document> get(UUID id) {
        return documentRepository.findById(id);
    }
}
