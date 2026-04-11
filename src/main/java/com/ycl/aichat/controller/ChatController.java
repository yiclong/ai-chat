package com.ycl.aichat.controller;

import com.ycl.aichat.entity.ChatMessageRequest;
import com.ycl.aichat.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(value = "/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatMessageRequest request) {
        SseEmitter emitter = new SseEmitter(60000L);
        String sessionId = (request.getSessionId() != null && !request.getSessionId().isEmpty()) 
                ? request.getSessionId() : UUID.randomUUID().toString();
        chatService.chatStream(request.getMessage(), request.getModel(), sessionId, emitter);
        return emitter;
    }

    @PostMapping("/stop/{sessionId}")
    public Map<String, Object> stopChat(@PathVariable String sessionId) {
        boolean success = chatService.stopChat(sessionId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "已停止对话" : "会话不存在或已结束");
        return result;
    }

    @GetMapping("/models")
    public List<Map<String, Object>> getModels() {
        return chatService.getModels();
    }
}