package com.ycl.aichat.dto;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String message;
    private String model;
}