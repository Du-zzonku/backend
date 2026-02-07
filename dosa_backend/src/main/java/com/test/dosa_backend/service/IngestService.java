package com.test.dosa_backend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.test.dosa_backend.domain.Document;
import com.test.dosa_backend.domain.DocumentChunk;
import com.test.dosa_backend.domain.DocumentStatus;
import com.test.dosa_backend.domain.IngestJob;
import com.test.dosa_backend.domain.IngestJobStatus;
import com.test.dosa_backend.openai.OpenAiClient;
import com.test.dosa_backend.rag.PdfTextExtractor;
import com.test.dosa_backend.rag.TextChunker;
import com.test.dosa_backend.rag.VectorStoreRepository;
import com.test.dosa_backend.repository.DocumentChunkRepository;
import com.test.dosa_backend.repository.DocumentRepository;
import com.test.dosa_backend.repository.IngestJobRepository;

@Service
public class IngestService {

    private final DocumentRepository documentRepository;
    private final IngestJobRepository ingestJobRepository;
    private final DocumentChunkRepository chunkRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final TextChunker textChunker;
    private final OpenAiClient openAiClient;
    private final VectorStoreRepository vectorStoreRepository;

    public IngestService(
            DocumentRepository documentRepository,
            IngestJobRepository ingestJobRepository,
            DocumentChunkRepository chunkRepository,
            PdfTextExtractor pdfTextExtractor,
            TextChunker textChunker,
            OpenAiClient openAiClient,
            VectorStoreRepository vectorStoreRepository
    ) {
        this.documentRepository = documentRepository;
        this.ingestJobRepository = ingestJobRepository;
        this.chunkRepository = chunkRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.textChunker = textChunker;
        this.openAiClient = openAiClient;
        this.vectorStoreRepository = vectorStoreRepository;
    }

    @Transactional
    public IngestJob createJob(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("document not found"));

        IngestJob job = new IngestJob(UUID.randomUUID(), doc, IngestJobStatus.PENDING, Instant.now());
        doc.setStatus(DocumentStatus.INGESTING);
        doc.setUpdatedAt(Instant.now());

        ingestJobRepository.save(job);
        documentRepository.save(doc);

        return job;
    }

    public IngestJob getJob(UUID jobId) {
        return ingestJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("job not found"));
    }

    @Async("ingestExecutor")
    public void runJobAsync(UUID jobId) {
        // We intentionally do NOT annotate this method @Transactional because it does long work.
        IngestJob job = getJob(jobId);
        UUID docId = job.getDocument().getId();

        try {
            markRunning(jobId);

            Document doc = documentRepository.findById(docId).orElseThrow();

            // 1) Extract & chunk
            List<PdfTextExtractor.PageText> pages = pdfTextExtractor.extract(doc.getStorageUri());
            List<TextChunker.TextChunk> chunks = textChunker.chunk(pages);

            // 2) Persist chunks
            List<DocumentChunk> savedChunks = saveChunks(docId, chunks);

            // 3) Embed & store vectors (batched)
            final int batchSize = 16;
            for (int i = 0; i < savedChunks.size(); i += batchSize) {
                int end = Math.min(savedChunks.size(), i + batchSize);
                List<DocumentChunk> batch = savedChunks.subList(i, end);
                List<String> texts = batch.stream().map(DocumentChunk::getContentText).toList();
                List<float[]> embs = openAiClient.embedTexts(texts);
                for (int j = 0; j < batch.size(); j++) {
                    vectorStoreRepository.upsertEmbedding(batch.get(j).getId(), embs.get(j), openAiClient.embeddingModel());
                }
            }

            markCompleted(jobId, savedChunks.size(), openAiClient.embeddingModel());
            markDocumentReady(docId);

        } catch (Exception e) {
            markFailed(jobId, e.getMessage());
            markDocumentFailed(docId);
        }
    }

    @Transactional
    protected void markRunning(UUID jobId) {
        IngestJob job = getJob(jobId);
        job.setStatus(IngestJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        ingestJobRepository.save(job);
    }

    @Transactional
    protected void markCompleted(UUID jobId, int chunkCount, String embeddingModel) {
        IngestJob job = getJob(jobId);
        job.setStatus(IngestJobStatus.COMPLETED);
        job.setFinishedAt(Instant.now());
        job.setChunkCount(chunkCount);
        job.setEmbeddingModel(embeddingModel);
        ingestJobRepository.save(job);
    }

    @Transactional
    protected void markFailed(UUID jobId, String error) {
        IngestJob job = getJob(jobId);
        job.setStatus(IngestJobStatus.FAILED);
        job.setFinishedAt(Instant.now());
        job.setErrorMessage(error);
        ingestJobRepository.save(job);
    }

    @Transactional
    protected void markDocumentReady(UUID docId) {
        Document doc = documentRepository.findById(docId).orElseThrow();
        doc.setStatus(DocumentStatus.READY);
        doc.setUpdatedAt(Instant.now());
        documentRepository.save(doc);
    }

    @Transactional
    protected void markDocumentFailed(UUID docId) {
        Document doc = documentRepository.findById(docId).orElseThrow();
        doc.setStatus(DocumentStatus.FAILED);
        doc.setUpdatedAt(Instant.now());
        documentRepository.save(doc);
    }

    @Transactional
    protected List<DocumentChunk> saveChunks(UUID docId, List<TextChunker.TextChunk> chunks) {
        Document doc = documentRepository.findById(docId).orElseThrow();
        Instant now = Instant.now();

        List<DocumentChunk> out = new ArrayList<>();
        for (TextChunker.TextChunk c : chunks) {
            DocumentChunk entity = new DocumentChunk(
                    UUID.randomUUID(),
                    doc,
                    c.chunkIndex(),
                    c.text(),
                    c.metaJson(),
                    now
            );
            out.add(chunkRepository.save(entity));
        }
        return out;
    }
}
