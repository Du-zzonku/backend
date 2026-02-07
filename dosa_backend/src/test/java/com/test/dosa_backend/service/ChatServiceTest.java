package com.test.dosa_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.test.dosa_backend.config.ChatPromptProperties;
import com.test.dosa_backend.dto.ChatDtos;
import com.test.dosa_backend.openai.OpenAiClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;

class ChatServiceTest {

    @Test
    void userMessage_applies_root_and_model_system_prompt_and_includes_history() {
        RagService ragService = mock(RagService.class);
        OpenAiClient openAiClient = mock(OpenAiClient.class);

        ChatPromptProperties props = new ChatPromptProperties();
        props.setRootSystemPrompt("ROOT_PROMPT");
        props.setModelSystemPrompts(Map.of("v4_engine", "V4_ENGINE_PROMPT"));

        ChatService chatService = new ChatService(
                ragService,
                openAiClient,
                props,
                "gpt-5-mini"
        );

        UUID docId = UUID.randomUUID();
        RagService.Citation citation = new RagService.Citation("S1", docId, "doc", "1", UUID.randomUUID(), 0.1);
        when(ragService.retrieve(anyString(), anyInt(), anyList()))
                .thenReturn(new RagService.RagResult("[S1] context", List.of(citation)));
        when(openAiClient.generateResponse(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn("assistant answer");

        List<ChatDtos.HistoryMessage> history = List.of(
                new ChatDtos.HistoryMessage("user", "prev q"),
                new ChatDtos.HistoryMessage("assistant", "prev a")
        );

        ChatService.ChatTurnResult result = chatService.userMessage(
                "new question",
                List.of(docId),
                List.of("https://example.com/a.png"),
                Map.of(
                        "model", Map.of("modelId", "v4_engine", "title", "V4 Engine"),
                        "parts", List.of(Map.of("partId", "CRANKSHAFT"), Map.of("partId", "PISTON"))
                ),
                history
        );

        ArgumentCaptor<String> instructionsCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OpenAiClient.ChatInputMessage>> messagesCaptor = (ArgumentCaptor<List<OpenAiClient.ChatInputMessage>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);

        verify(openAiClient).generateResponse(anyString(), instructionsCaptor.capture(), messagesCaptor.capture(), anyInt());

        String instructions = instructionsCaptor.getValue();
        assertThat(instructions).contains("ROOT_PROMPT");
        assertThat(instructions).contains("V4_ENGINE_PROMPT");
        assertThat(instructions).contains("v4_engine");
        assertThat(instructions).contains("Sources");

        List<OpenAiClient.ChatInputMessage> messages = messagesCaptor.getValue();
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).role()).isEqualTo("user");
        assertThat(messages.get(1).role()).isEqualTo("assistant");
        assertThat(messages.get(2).role()).isEqualTo("user");
        assertThat(messages.get(2).imageUrls()).containsExactly("https://example.com/a.png");

        assertThat(result.answer()).isEqualTo("assistant answer");
        assertThat(result.citations()).hasSize(1);
    }
}
