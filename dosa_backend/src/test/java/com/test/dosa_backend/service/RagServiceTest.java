package com.test.dosa_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.test.dosa_backend.openai.OpenAiClient;
import com.test.dosa_backend.rag.VectorStoreRepository;
import com.test.dosa_backend.repository.DocumentChunkRepository;

class RagServiceTest {

    @Test
    void retrieve_with_empty_document_ids_searches_all_documents() {
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        VectorStoreRepository vectorStoreRepository = mock(VectorStoreRepository.class);
        DocumentChunkRepository documentChunkRepository = mock(DocumentChunkRepository.class);

        RagService ragService = new RagService(openAiClient, vectorStoreRepository, documentChunkRepository);

        when(documentChunkRepository.count()).thenReturn(10L);
        when(openAiClient.embedTexts(any())).thenReturn(List.of(new float[] {0.1f, 0.2f}));
        VectorStoreRepository.SearchHit hit = new VectorStoreRepository.SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                0,
                "chunk text",
                "{\"startPage\":1,\"endPage\":1}",
                "doc title",
                0.12
        );
        when(vectorStoreRepository.similaritySearch(any(float[].class), eq(5), isNull())).thenReturn(List.of(hit));

        RagService.RagResult result = ragService.retrieve("네트워크 설명", 5, List.of());

        verify(vectorStoreRepository).similaritySearch(any(float[].class), anyInt(), isNull());
        assertThat(result.contextText()).contains("doc title");
        assertThat(result.citations()).hasSize(1);
    }
}

