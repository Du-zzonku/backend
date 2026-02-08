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

import java.nio.charset.StandardCharsets;
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
        assertThat(result.appliedSystemPrompt()).isNotNull();
        assertThat(result.appliedSystemPrompt().modelId()).isEqualTo("v4_engine");
        assertThat(result.appliedSystemPrompt().modelSystemPromptApplied()).isTrue();
        assertThat(result.appliedSystemPrompt().modelSystemPrompt()).isEqualTo("V4_ENGINE_PROMPT");
    }

    @Test
    void userMessage_does_not_infer_model_id_when_metadata_missing() {
        RagService ragService = mock(RagService.class);
        OpenAiClient openAiClient = mock(OpenAiClient.class);

        ChatPromptProperties props = new ChatPromptProperties();
        props.setRootSystemPrompt("ROOT_PROMPT");
        props.setModelSystemPrompts(Map.of(
                "v4_engine", "V4_ENGINE_PROMPT",
                "drone", "DRONE_PROMPT"
        ));

        ChatService chatService = new ChatService(
                ragService,
                openAiClient,
                props,
                "gpt-5-mini"
        );
        when(openAiClient.generateResponse(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn("assistant answer");

        ChatService.ChatTurnResult result = chatService.userMessage(
                "v4 엔진의 토크 전달 흐름 설명해줘",
                List.of(),
                List.of(),
                null,
                null
        );

        ArgumentCaptor<String> instructionsCaptor = ArgumentCaptor.forClass(String.class);
        verify(openAiClient).generateResponse(anyString(), instructionsCaptor.capture(), anyList(), anyInt());
        assertThat(instructionsCaptor.getValue()).doesNotContain("V4_ENGINE_PROMPT");
        assertThat(result.appliedSystemPrompt().modelId()).isNull();
        assertThat(result.appliedSystemPrompt().modelSystemPromptApplied()).isFalse();
        assertThat(result.appliedSystemPrompt().modelSystemPrompt()).isNull();
    }

    @Test
    void userMessage_recovers_mojibake_prompt_text() {
        RagService ragService = mock(RagService.class);
        OpenAiClient openAiClient = mock(OpenAiClient.class);

        String original = "당신은 과학/공학 학습용 3D 뷰어 서비스의 AI 튜터입니다.";
        String mojibake = new String(original.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);

        ChatPromptProperties props = new ChatPromptProperties();
        props.setRootSystemPrompt(mojibake);
        props.setModelSystemPrompts(Map.of());

        ChatService chatService = new ChatService(
                ragService,
                openAiClient,
                props,
                "gpt-5-mini"
        );
        when(openAiClient.generateResponse(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn("assistant answer");

        ChatService.ChatTurnResult result = chatService.userMessage(
                "기본 동작 설명",
                List.of(),
                List.of(),
                null,
                null
        );

        assertThat(result.appliedSystemPrompt().rootSystemPrompt()).isEqualTo(original);
    }
}
