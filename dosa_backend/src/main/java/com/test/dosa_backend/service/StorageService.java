package com.test.dosa_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    private final Path baseDir;

    public StorageService(@Value("${app.storage.base-dir}") String baseDir) {
        this.baseDir = Path.of(baseDir).toAbsolutePath().normalize();
    }

    public StoredFile savePdf(UUID documentId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }
        String original = file.getOriginalFilename();
        String filename = (original != null && !original.isBlank()) ? original : (documentId + ".pdf");
        if (!filename.toLowerCase().endsWith(".pdf")) {
            filename = filename + ".pdf";
        }

        Path docDir = baseDir.resolve("documents").resolve(documentId.toString());
        Files.createDirectories(docDir);

        Path target = docDir.resolve(filename);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return new StoredFile(target.toString(), filename, Files.size(target));
    }

    public record StoredFile(String path, String filename, long sizeBytes) {}
}
