package com.ycl.aichat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ycl.aichat.dto.ChatRequest;
import com.ycl.aichat.dto.Message;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ChatService {
    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model}")
    private String model;

    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;

    public ChatService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void chatStream(String userMessage, SseEmitter emitter) {
        chatStream(userMessage, null, emitter);
    }

    public void chatStream(String userMessage, String modelParam, SseEmitter emitter) {
        ChatRequest request = new ChatRequest();
        String useModel = (modelParam != null && !modelParam.isEmpty()) ? modelParam : model;
        request.setModel(useModel);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", userMessage));
        request.setMessages(messages);

        String url = baseUrl + "/chat/completions";

        log.info("=== Stream Request ===");
        log.info("URL: {}", url);
        log.info("Model: {}", useModel);

        try {
            String requestBody = objectMapper.writeValueAsString(request);
            log.info("Request Body: {}", requestBody);

            Request httpRequest = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            EventSource.Factory factory = EventSources.createFactory(okHttpClient);
            factory.newEventSource(httpRequest, new EventSourceListener() {
                @Override
                public void onOpen(EventSource eventSource, Response response) {
                    log.info("SSE connection opened");
                }

                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    log.info("SSE event: id={}, type={}, data={}", id, type, data);
                    if ("[DONE]".equals(data)) {
                        log.info("Stream done");
                        emitter.complete();
                        return;
                    }
                    try {
                        String content = objectMapper.readTree(data)
                                .path("choices")
                                .get(0)
                                .path("delta")
                                .path("content")
                                .asText("");
                        if (!content.isEmpty()) {
                            log.info("Send content: {}", content);
                            emitter.send(SseEmitter.event().data(content));
                        }
                    } catch (Exception e) {
                        log.error("Parse error: {}", e.getMessage());
                    }
                }

                @Override
                public void onClosed(EventSource eventSource) {
                    log.info("SSE connection closed");
                    emitter.complete();
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    log.error("SSE failure: {}", t.getMessage());
                    if (response != null) {
                        try {
                            String body = response.body().string();
                            log.error("Error response: {}", body);
                            emitter.send("Error: " + body);
                        } catch (IOException e) {
                            log.error("Read error response failed", e);
                        }
                    }
                    emitter.completeWithError(t);
                }
            });
        } catch (Exception e) {
            log.error("Request error: ", e);
            emitter.completeWithError(e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getModels() {
        String url = baseUrl + "/models";
        try {
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .get()
                    .build();

            try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Get models failed: {}", response.code());
                    return new ArrayList<>();
                }
                String body = response.body().string();
                Map<String, Object> result = objectMapper.readValue(body, Map.class);
                Object data = result.get("data");
                if (data instanceof List) {
                    return (List<Map<String, Object>>) data;
                }
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("Get models error: ", e);
            return new ArrayList<>();
        }
    }
}