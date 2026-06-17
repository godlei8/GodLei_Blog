package cn.godlei.blogserver.service.assistant.client;

import cn.godlei.blogpojo.dto.response.assistant.AssistantRuntimeTestResultDTO;
import cn.godlei.blogserver.service.assistant.config.AssistantResolvedRuntimeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleChatClientImplTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldParseStreamingDeltasAndIgnoreEmptyChunks() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> respondEventStream(exchange,
                "data: {\"choices\":[{\"delta\":{\"role\":\"assistant\"}}]}\n\n"
                        + "data: {\"choices\":[{\"delta\":{}}]}\n\n"
                        + "data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}\n\n"
                        + "data: {\"choices\":[{\"delta\":{\"content\":\" world\"}}]}\n\n"
                        + "data: [DONE]\n\n"
        ));
        server.start();

        OpenAiCompatibleChatClientImpl client = new OpenAiCompatibleChatClientImpl(new ObjectMapper());
        AssistantResolvedRuntimeConfig runtimeConfig = createRuntimeConfig(baseUrl());

        List<String> deltas = new ArrayList<>();
        client.streamChat(runtimeConfig, List.of(Map.of("role", "user", "content", "hi")), deltas::add);

        assertIterableEquals(List.of("Hello", " world"), deltas);
    }

    @Test
    void shouldSummarizeUnauthorizedTestResponse() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> respondJson(exchange, 401, "{\"error\":{\"message\":\"invalid api key\"}}"));
        server.start();

        OpenAiCompatibleChatClientImpl client = new OpenAiCompatibleChatClientImpl(new ObjectMapper());
        AssistantResolvedRuntimeConfig runtimeConfig = createRuntimeConfig(baseUrl());

        AssistantRuntimeTestResultDTO result = client.testConnection(runtimeConfig, "system prompt");

        assertFalse(result.isSuccess());
        assertEquals(401, result.getStatusCode());
        assertEquals("invalid api key", result.getMessage());
    }

    @Test
    void shouldReturnSuccessOnTestConnection() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> respondJson(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}"));
        server.start();

        OpenAiCompatibleChatClientImpl client = new OpenAiCompatibleChatClientImpl(new ObjectMapper());
        AssistantResolvedRuntimeConfig runtimeConfig = createRuntimeConfig(baseUrl());

        AssistantRuntimeTestResultDTO result = client.testConnection(runtimeConfig, "");

        assertTrue(result.isSuccess());
        assertEquals(200, result.getStatusCode());
        assertTrue(result.getMessage().contains("OK"));
    }

    private AssistantResolvedRuntimeConfig createRuntimeConfig(String baseUrl) {
        AssistantResolvedRuntimeConfig config = new AssistantResolvedRuntimeConfig();
        config.setProvider("deepseek");
        config.setBaseUrl(baseUrl);
        config.setModel("deepseek-v4-pro");
        config.setApiKey("secret-key");
        config.setTemperature(0.7D);
        config.setTopP(0.95D);
        config.setReadTimeoutMs(15000L);
        config.setConnectTimeoutMs(5000L);
        return config;
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    private void respondEventStream(HttpExchange exchange, String body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream; charset=utf-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
