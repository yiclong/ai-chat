package com.ycl.aichat.controller;

import com.ycl.aichat.dto.ChatMessageRequest;
import com.ycl.aichat.service.ChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model}")
    private String model;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/config")
    public Map<String, String> getConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("baseUrl", baseUrl);
        config.put("model", model);
        return config;
    }

    @PostMapping(value = "/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatMessageRequest request) {
        SseEmitter emitter = new SseEmitter(60000L);
        chatService.chatStream(request.getMessage(), request.getModel(), emitter);
        return emitter;
    }

    @GetMapping("/models")
    public List<Map<String, Object>> getModels() {
        return chatService.getModels();
    }
}