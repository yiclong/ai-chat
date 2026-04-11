package com.ycl.aichat.controller;

import com.ycl.aichat.dto.ChatMessageRequest;
import com.ycl.aichat.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

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
        chatService.chatStream(request.getMessage(), request.getModel(), emitter);
        return emitter;
    }

    @GetMapping("/models")
    public List<Map<String, Object>> getModels() {
        return chatService.getModels();
    }
}