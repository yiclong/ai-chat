package com.ycl.aichat.entity;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String message;
    private String model;
}