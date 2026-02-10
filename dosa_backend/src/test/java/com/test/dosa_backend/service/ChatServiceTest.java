package com.test.dosa_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.test.dosa_backend.config.ChatPromptProperties;
import com.test.dosa_backend.domain.Model;
import com.test.dosa_backend.domain.Part;
import com.test.dosa_backend.dto.ChatDtos;
import com.test.dosa_backend.openai.OpenAiClient;
import com.test.dosa_backend.repository.PartRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class ChatServiceTest {

    @Test
    void userMessage_applies_root_and_model_system_prompt_and_includes_history() {
        RagService ragService = mock(RagService.class);
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        PartRepository partRepository = mock(PartRepository.class);

        ChatPromptProperties props = new ChatPromptProperties();
        props.setRootSystemPrompt("ROOT_PROMPT");
        props.setModelSystemPrompts(Map.of("v4_engine", "V4_ENGINE_PROMPT"));

        when(partRepository.findAllById(anyList())).thenReturn(List.of());

        ChatService chatService = new ChatService(
                ragService,
                openAiClient,
                partRepository,
                props,
                "gpt-5-mini",
                3,
                1200
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
                true,
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
        PartRepository partRepository = mock(PartRepository.class);

        ChatPromptProperties props = new ChatPromptProperties();
        props.setRootSystemPrompt("ROOT_PROMPT");
        props.setModelSystemPrompts(Map.of(
                "v4_engine", "V4_ENGINE_PROMPT",
                "drone", "DRONE_PROMPT"
        ));

        when(partRepository.findAllById(anyList())).thenReturn(List.of());

        ChatService chatService = new ChatService(
                ragService,
                openAiClient,
                partRepository,
                props,
                "gpt-5-mini",
                3,
                1200
        );
        when(openAiClient.generateResponse(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn("assistant answer");

        ChatService.ChatTurnResult result = chatService.userMessage(
                "v4 엔진의 토크 전달 흐름 설명해줘",
                false,
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
        PartRepository partRepository = mock(PartRepository.class);

        String original = "당신은 과학/공학 학습용 3D 뷰어 서비스의 AI 튜터입니다.";
        String mojibake = new String(original.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);

        ChatPromptProperties props = new ChatPromptProperties();
        props.setRootSystemPrompt(mojibake);
        props.setModelSystemPrompts(Map.of());

        when(partRepository.findAllById(anyList())).thenReturn(List.of());

        ChatService chatService = new ChatService(
                ragService,
                openAiClient,
                partRepository,
                props,
                "gpt-5-mini",
                3,
                1200
        );
        when(openAiClient.generateResponse(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn("assistant answer");

        ChatService.ChatTurnResult result = chatService.userMessage(
                "기본 동작 설명",
                false,
                List.of(),
                List.of(),
                null,
                null
        );

        assertThat(result.appliedSystemPrompt().rootSystemPrompt()).isEqualTo(original);
    }

    @Test
    void userMessage_enriches_parts_from_db_and_excludes_material_type() {
        RagService ragService = mock(RagService.class);
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        PartRepository partRepository = mock(PartRepository.class);

        ChatPromptProperties props = new ChatPromptProperties();
        props.setRootSystemPrompt("ROOT_PROMPT");
        props.setModelSystemPrompts(Map.of("v4_engine", "V4_ENGINE_PROMPT"));

        Part crankshaft = mock(Part.class);
        Model model = mock(Model.class);
        when(model.getModelId()).thenReturn("v4_engine");
        when(crankshaft.getPartId()).thenReturn("CRANKSHAFT");
        when(crankshaft.getModel()).thenReturn(model);
        when(crankshaft.getDisplayNameKo()).thenReturn("크랭크샤프트");
        when(crankshaft.getSummary()).thenReturn("엔진의 중심 축");

        when(partRepository.findAllById(anyList())).thenReturn(List.of(crankshaft));
        when(openAiClient.generateResponse(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn("assistant answer");

        ChatService chatService = new ChatService(
                ragService,
                openAiClient,
                partRepository,
                props,
                "gpt-5-mini",
                3,
                1200
        );

        chatService.userMessage(
                "설명해줘",
                false,
                List.of(),
                List.of(),
                Map.of(
                        "model", Map.of("modelId", "v4_engine", "title", "V4 Engine Assembly"),
                        "parts", List.of(Map.of("partId", "CRANKSHAFT", "materialType", "METAL_STEEL_MACHINED"))
                ),
                null
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OpenAiClient.ChatInputMessage>> messagesCaptor = (ArgumentCaptor<List<OpenAiClient.ChatInputMessage>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        verify(openAiClient).generateResponse(anyString(), anyString(), messagesCaptor.capture(), anyInt());
        String finalPrompt = messagesCaptor.getValue().get(messagesCaptor.getValue().size() - 1).text();

        assertThat(finalPrompt).contains("\"partId\":\"CRANKSHAFT\"");
        assertThat(finalPrompt).contains("\"displayNameKo\":\"크랭크샤프트\"");
        assertThat(finalPrompt).contains("\"summary\":\"엔진의 중심 축\"");
        assertThat(finalPrompt).doesNotContain("materialType");
        assertThat(finalPrompt).contains("\"modelId\":\"v4_engine\"");
        assertThat(finalPrompt).contains("\"title\":\"V4 Engine Assembly\"");
        assertThat(finalPrompt).doesNotContain("\"overview\"");
        assertThat(finalPrompt).doesNotContain("\"theory\"");
    }

    @Test
    void userMessage_useRag_true_with_empty_document_ids_queries_all_documents_and_uses_multi_source_query() {
        RagService ragService = mock(RagService.class);
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        PartRepository partRepository = mock(PartRepository.class);

        ChatPromptProperties props = new ChatPromptProperties();
        props.setRootSystemPrompt("ROOT_PROMPT");
        props.setModelSystemPrompts(Map.of("v4_engine", "V4_ENGINE_PROMPT"));

        Part crankshaft = mock(Part.class);
        Model model = mock(Model.class);
        when(model.getModelId()).thenReturn("v4_engine");
        when(crankshaft.getPartId()).thenReturn("CRANKSHAFT");
        when(crankshaft.getModel()).thenReturn(model);
        when(crankshaft.getDisplayNameKo()).thenReturn("크랭크샤프트");
        when(crankshaft.getSummary()).thenReturn("엔진의 중심 축");

        when(partRepository.findAllById(anyList())).thenReturn(List.of(crankshaft));
        when(ragService.retrieve(anyString(), anyInt(), any())).thenReturn(new RagService.RagResult("", List.of()));
        when(openAiClient.generateResponse(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn("assistant answer");

        ChatService chatService = new ChatService(
                ragService,
                openAiClient,
                partRepository,
                props,
                "gpt-5-mini",
                3,
                1200
        );

        chatService.userMessage(
                "v4 엔진 설명해줘",
                true,
                List.of(),
                List.of(),
                Map.of(
                        "model", Map.of("modelId", "v4_engine", "title", "V4 Engine Assembly"),
                        "parts", List.of(Map.of("partId", "CRANKSHAFT"))
                ),
                List.of(
                        new ChatDtos.HistoryMessage("user", "이전 질문"),
                        new ChatDtos.HistoryMessage("assistant", "이전 답변")
                )
        );

        ArgumentCaptor<String> ragQueryCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UUID>> docIdsCaptor = (ArgumentCaptor<List<UUID>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        verify(ragService).retrieve(ragQueryCaptor.capture(), anyInt(), docIdsCaptor.capture());

        assertThat(docIdsCaptor.getValue()).isNull();
        assertThat(ragQueryCaptor.getValue()).contains("User question");
        assertThat(ragQueryCaptor.getValue()).contains("Recent conversation");
        assertThat(ragQueryCaptor.getValue()).contains("modelId: v4_engine");
        assertThat(ragQueryCaptor.getValue()).contains("CRANKSHAFT / 크랭크샤프트 / 엔진의 중심 축");
    }
}
