package com.test.dosa_backend.config;

import java.time.Duration;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class AppConfig {

    @Bean(name = "ingestExecutor")
    public Executor ingestExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setThreadNamePrefix("ingest-");
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(4);
        exec.setQueueCapacity(100);
        exec.initialize();
        return exec;
    }

    @Bean
    public WebClient openaiWebClient(
            @Value("${openai.base-url}") String baseUrl,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.http.max-connections:100}") int maxConnections,
            @Value("${openai.http.pending-acquire-timeout-seconds:30}") int pendingAcquireTimeoutSeconds,
            @Value("${openai.http.response-timeout-seconds:60}") int responseTimeoutSeconds
    ) {
        // Increase memory for potentially large JSON (embeddings)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                .build();

        ConnectionProvider provider = ConnectionProvider.builder("openai-http")
                .maxConnections(Math.max(10, maxConnections))
                .pendingAcquireTimeout(Duration.ofSeconds(Math.max(1, pendingAcquireTimeoutSeconds)))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .compress(true)
                .keepAlive(true)
                .responseTimeout(Duration.ofSeconds(Math.max(1, responseTimeoutSeconds)));

        WebClient.Builder builder = WebClient.builder()
                .exchangeStrategies(strategies)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("User-Agent", "dosa-backend/1.0");

        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }

        return builder.build();
    }

    @Bean
    public Duration openaiTimeout() {
        return Duration.ofSeconds(60);
    }
}
