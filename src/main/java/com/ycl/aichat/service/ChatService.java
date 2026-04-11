package com.ycl.aichat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ycl.aichat.entity.ChatRequest;
import com.ycl.aichat.entity.Message;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<String, EventSource> activeEventSources = new ConcurrentHashMap<>();

    public ChatService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void chatStream(String userMessage, String modelParam, String sessionId, SseEmitter emitter) {
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
            EventSource eventSource = factory.newEventSource(httpRequest, new EventSourceListener() {
                @Override
                public void onOpen(EventSource eventSource, Response response) {
                    log.info("SSE connection opened");
                    activeEventSources.put(sessionId, eventSource);
                }

                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    log.info("SSE event: id={}, type={}, data={}", id, type, data);
                    if ("[DONE]".equals(data)) {
                        log.info("Stream done");
                        emitter.complete();
                        activeEventSources.remove(sessionId);
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
                    activeEventSources.remove(sessionId);
                }

                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    log.error("SSE failure: {}", t != null ? t.getMessage() : "unknown");
                    activeEventSources.remove(sessionId);
                    if (response != null && response.body() != null) {
                        try {
                            String body = response.body().string();
                            log.error("Error response: {}", body);
                            emitter.send("Error: " + body);
                        } catch (IOException e) {
                            log.error("Read error response failed", e);
                        }
                    }
                    emitter.completeWithError(t != null ? t : new RuntimeException("Unknown error"));
                }
            });
        } catch (Exception e) {
            log.error("Request error: ", e);
            emitter.completeWithError(e);
        }
    }

    public List<Map<String, Object>> getModels() {
        List<Map<String, Object>> models = new ArrayList<>();
        String[] modelIds = {"glm-5", "minmax-m2.7", "kimi2.5", "qwen3.6plus"};
        for (String modelId : modelIds) {
            Map<String, Object> model = new HashMap<>();
            model.put("id", modelId);
            models.add(model);
        }
        return models;
    }

    public boolean stopChat(String sessionId) {
        EventSource eventSource = activeEventSources.get(sessionId);
        if (eventSource != null) {
            eventSource.cancel();
            activeEventSources.remove(sessionId);
            log.info("Chat session stopped: {}", sessionId);
            return true;
        }
        return false;
    }
}