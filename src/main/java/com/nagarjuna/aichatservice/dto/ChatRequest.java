package com.nagarjuna.aichatservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(

        @NotBlank(message = "Message must not be blank")
        String message,

        @NotBlank(message = "Session ID should not be blank")
        String sessionId // Client generates UUID and sends on every request
) {}
