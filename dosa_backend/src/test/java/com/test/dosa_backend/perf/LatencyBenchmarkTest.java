package com.test.dosa_backend.perf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import com.test.dosa_backend.config.ChatPromptProperties;
import com.test.dosa_backend.domain.Model;
import com.test.dosa_backend.domain.Part;
import com.test.dosa_backend.dto.ChatDtos;
import com.test.dosa_backend.openai.OpenAiClient;
import com.test.dosa_backend.repository.PartRepository;
import com.test.dosa_backend.service.ChatService;
import com.test.dosa_backend.service.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

class LatencyBenchmarkTest {

    @Test
    void benchmark_chat_service_user_message_overhead() {
        RagService ragService = mock(RagService.class);
        OpenAiClient openAiClient = mock(OpenAiClient.class);
        PartRepository partRepository = mock(PartRepository.class);

        ChatPromptProperties props = new ChatPromptProperties();
        props.setRootSystemPrompt("ROOT_PROMPT");
        props.setModelSystemPrompts(Map.of("v4_engine", "V4_ENGINE_PROMPT"));

        List<Part> mockParts = mockParts(12);
        when(partRepository.findAllById(anyList())).thenReturn(mockParts);
        when(ragService.retrieve(anyString(), anyInt(), any()))
                .thenReturn(new RagService.RagResult(sampleRagContext(2), List.of()));
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

        List<ChatDtos.HistoryMessage> history = sampleHistory(30, 180);
        Map<String, Object> metadata = sampleMetadata(12);
        List<String> images = List.of("https://example.com/image-a.png");

        int warmup = 400;
        int iterations = 3000;

        for (int i = 0; i < warmup; i++) {
            chatService.userMessage(
                    "v4 엔진의 토크 전달 과정을 단계별로 설명해줘. " + i,
                    true,
                    List.of(UUID.randomUUID()),
                    images,
                    metadata,
                    history
            );
        }

        long[] times = new long[iterations];
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            ChatService.ChatTurnResult result = chatService.userMessage(
                    "v4 엔진의 토크 전달 과정을 단계별로 설명해줘. " + i,
                    true,
                    List.of(UUID.randomUUID()),
                    images,
                    metadata,
                    history
            );
            times[i] = System.nanoTime() - start;
            assertThat(result.answer()).isEqualTo("assistant answer");
        }

        printStats("CHAT_SERVICE_USER_MESSAGE", times);
    }

    @Test
    void benchmark_openai_client_responses_path() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/responses", exchange -> {
            byte[] response = "{\"status\":\"completed\",\"output_text\":\"assistant answer\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            WebClient webClient = WebClient.builder()
                    .baseUrl("http://127.0.0.1:" + port + "/v1")
                    .build();

            OpenAiClient client = new OpenAiClient(
                    webClient,
                    new ObjectMapper(),
                    Duration.ofSeconds(10),
                    "text-embedding-3-small",
                    0,
                    32000,
                    128000,
                    "low",
                    64000
            );

            String longText = randomText(2400);
            List<OpenAiClient.ChatInputMessage> messages = List.of(
                    new OpenAiClient.ChatInputMessage("user", longText, List.of())
            );

            int warmup = 80;
            int iterations = 400;

            for (int i = 0; i < warmup; i++) {
                String out = client.generateResponse("gpt-5-mini", "SYSTEM INSTRUCTIONS", messages, 700);
                assertThat(out).isEqualTo("assistant answer");
            }

            long[] times = new long[iterations];
            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                String out = client.generateResponse("gpt-5-mini", "SYSTEM INSTRUCTIONS", messages, 700);
                times[i] = System.nanoTime() - start;
                assertThat(out).isEqualTo("assistant answer");
            }

            printStats("OPENAI_CLIENT_RESPONSES", times);
        } finally {
            server.stop(0);
        }
    }

    private static void printStats(String name, long[] nanos) {
        long[] sorted = nanos.clone();
        java.util.Arrays.sort(sorted);
        long sum = 0L;
        for (long n : sorted) sum += n;
        double avgMs = sum / (double) sorted.length / 1_000_000.0;
        double p50Ms = sorted[(int) (sorted.length * 0.50)] / 1_000_000.0;
        double p95Ms = sorted[(int) (sorted.length * 0.95)] / 1_000_000.0;
        double p99Ms = sorted[(int) (sorted.length * 0.99)] / 1_000_000.0;
        System.out.printf(
                "BENCH_%s avg=%.3fms p50=%.3fms p95=%.3fms p99=%.3fms n=%d%n",
                name, avgMs, p50Ms, p95Ms, p99Ms, sorted.length
        );
    }

    private static List<ChatDtos.HistoryMessage> sampleHistory(int turns, int charsPerMessage) {
        List<ChatDtos.HistoryMessage> out = new ArrayList<>();
        for (int i = 0; i < turns; i++) {
            String role = (i % 2 == 0) ? "user" : "assistant";
            out.add(new ChatDtos.HistoryMessage(role, randomText(charsPerMessage)));
        }
        return out;
    }

    private static String sampleRagContext(int sourceCount) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= sourceCount; i++) {
            sb.append("[S").append(i).append("] Document ").append(i).append("\n");
            sb.append(randomText(600)).append("\n\n");
        }
        return sb.toString().trim();
    }

    private static Map<String, Object> sampleMetadata(int partCount) {
        List<Map<String, Object>> parts = new ArrayList<>();
        for (int i = 1; i <= partCount; i++) {
            parts.add(Map.of("partId", "PART_" + i));
        }
        return Map.of(
                "model", Map.of("modelId", "v4_engine", "title", "V4 Engine Assembly"),
                "parts", parts,
                "extra", Map.of("client", "perf", "session", "bench")
        );
    }

    private static List<Part> mockParts(int count) {
        List<Part> out = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Part p = mock(Part.class);
            Model m = mock(Model.class);
            when(m.getModelId()).thenReturn("v4_engine");
            when(p.getPartId()).thenReturn("PART_" + i);
            when(p.getModel()).thenReturn(m);
            when(p.getDisplayNameKo()).thenReturn("부품" + i);
            when(p.getSummary()).thenReturn("요약 " + i);
            out.add(p);
        }
        return out;
    }

    private static String randomText(int length) {
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789 ";
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
