package com.test.dosa_backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.test.dosa_backend.openai.OpenAiClient;
import com.test.dosa_backend.openai.OpenAiException;
import com.test.dosa_backend.repository.DocumentChunkRepository;
import com.test.dosa_backend.rag.VectorStoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final OpenAiClient openAiClient;
    private final VectorStoreRepository vectorStoreRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public RagService(OpenAiClient openAiClient,
                      VectorStoreRepository vectorStoreRepository,
                      DocumentChunkRepository documentChunkRepository) {
        this.openAiClient = openAiClient;
        this.vectorStoreRepository = vectorStoreRepository;
        this.documentChunkRepository = documentChunkRepository;
    }

    public RagResult retrieve(String query, int topK, List<UUID> documentIds) {
        if (query == null || query.isBlank()) {
            return emptyResult();
        }
        List<UUID> normalizedDocumentIds = normalizeDocumentIds(documentIds);

        // If nothing has been ingested yet, don't call embeddings at all.
        // This avoids unnecessary embeddings latency for empty corpora.
        if (!hasSearchableChunks(normalizedDocumentIds)) {
            return emptyResult();
        }

        try {
            float[] qEmb = openAiClient.embedTexts(List.of(query)).get(0);
            List<VectorStoreRepository.SearchHit> hits = vectorStoreRepository.similaritySearch(qEmb, topK, normalizedDocumentIds);
            List<Citation> citations = new ArrayList<>();
            StringBuilder ctx = new StringBuilder();

            for (int i = 0; i < hits.size(); i++) {
                VectorStoreRepository.SearchHit h = hits.get(i);
                String tag = "S" + (i + 1);
                String pages = extractPageRange(h.metaJson());
                citations.add(new Citation(tag, h.documentId(), h.documentTitle(), pages, h.chunkId(), h.distance()));
                ctx.append("[").append(tag).append("] ")
                        .append(h.documentTitle())
                        .append(pages.isBlank() ? "" : " (p." + pages + ")")
                        .append("\n")
                        .append(h.contentText())
                        .append("\n\n");
            }

            return new RagResult(ctx.toString().trim(), citations);
        } catch (OpenAiException e) {
            log.warn("RAG embeddings failed; returning empty context.", e);
            return emptyResult();
        }
    }

    private String extractPageRange(String metaJson) {
        try {
            JsonNode n = mapper.readTree(metaJson);
            int sp = n.path("startPage").asInt(-1);
            int ep = n.path("endPage").asInt(-1);
            if (sp > 0 && ep > 0) {
                if (sp == ep) return String.valueOf(sp);
                return sp + "-" + ep;
            }
        } catch (Exception ignored) {}
        return "";
    }

    public record RagResult(String contextText, List<Citation> citations) {}

    public record Citation(
            String tag,
            UUID documentId,
            String documentTitle,
            String pages,
            UUID chunkId,
            double distance
    ) {}

    private RagResult emptyResult() {
        return new RagResult("", List.of());
    }

    private boolean hasSearchableChunks(List<UUID> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return documentChunkRepository.existsByDocumentIsNotNull();
        }
        return documentChunkRepository.existsByDocument_IdIn(documentIds);
    }

    private List<UUID> normalizeDocumentIds(List<UUID> documentIds) {
        if (documentIds == null) {
            return null;
        }
        List<UUID> filtered = documentIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return filtered.isEmpty() ? null : filtered;
    }
}
