package com.test.dosa_backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.test.dosa_backend.dto.ChatDtos;
import com.test.dosa_backend.service.ChatService;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChatService chatService;

    @Test
    void message_json_accepts_model_parts_history_and_image_urls() throws Exception {
        when(chatService.userMessage(anyString(), any(), any(), any(), any()))
                .thenReturn(new ChatService.ChatTurnResult("ok", List.of()));

        String body = """
                {
                  "message": "hello",
                  "documentIds": [],
                  "imageUrls": ["https://example.com/a.png"],
                  "extraMetadata": {"userId":"u-1","client":"swagger"},
                  "model": {
                    "modelId": "v4_engine",
                    "title": "V4 Engine Assembly"
                  },
                  "parts": [
                    {"partId":"CRANKSHAFT","displayNameKo":"크랭크샤프트"},
                    {"partId":"PISTON","displayNameKo":"피스톤"}
                  ],
                  "history": [
                    {"role":"user","content":"prev q"},
                    {"role":"assistant","content":"prev a"}
                  ]
                }
                """;

        mvc.perform(post("/v1/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("ok"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> metaCaptor = (ArgumentCaptor<Map<String, Object>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> imagesCaptor = (ArgumentCaptor<List<String>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatDtos.HistoryMessage>> historyCaptor = (ArgumentCaptor<List<ChatDtos.HistoryMessage>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);

        verify(chatService).userMessage(eq("hello"), any(), imagesCaptor.capture(), metaCaptor.capture(), historyCaptor.capture());

        assertThat(imagesCaptor.getValue()).containsExactly("https://example.com/a.png");
        assertThat(metaCaptor.getValue()).containsEntry("userId", "u-1").containsEntry("client", "swagger");
        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) metaCaptor.getValue().get("model");
        assertThat(model).containsEntry("modelId", "v4_engine");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) metaCaptor.getValue().get("parts");
        assertThat(parts).hasSize(2);
        assertThat(historyCaptor.getValue()).hasSize(2);
        assertThat(historyCaptor.getValue().get(0).role()).isEqualTo("user");
    }

    @Test
    void message_multipart_accepts_uploaded_image_metadata_and_history_json() throws Exception {
        when(chatService.userMessage(anyString(), any(), any(), any(), any()))
                .thenReturn(new ChatService.ChatTurnResult("ok", List.of()));

        MockMultipartFile message = new MockMultipartFile(
                "message",
                "",
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes()
        );
        MockMultipartFile extraMetadata = new MockMultipartFile(
                "extraMetadata",
                "",
                MediaType.TEXT_PLAIN_VALUE,
                "{\"source\":\"swagger\",\"model\":{\"modelId\":\"v4_engine\"},\"parts\":[{\"partId\":\"CRANKSHAFT\"}]}".getBytes()
        );
        MockMultipartFile history = new MockMultipartFile(
                "history",
                "",
                MediaType.TEXT_PLAIN_VALUE,
                "[{\"role\":\"user\",\"content\":\"prev q\"}]".getBytes()
        );
        MockMultipartFile image = new MockMultipartFile(
                "images",
                "a.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        mvc.perform(multipart("/v1/chat/messages:multipart")
                        .file(message)
                        .file(extraMetadata)
                        .file(history)
                        .file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("ok"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> imagesCaptor = (ArgumentCaptor<List<String>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);

        verify(chatService).userMessage(eq("hello"), any(), imagesCaptor.capture(), any(), any());

        assertThat(imagesCaptor.getValue()).hasSize(1);
        assertThat(imagesCaptor.getValue().get(0)).startsWith("data:image/png;base64,");
    }
}
