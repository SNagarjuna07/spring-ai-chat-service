package com.nagarjuna.aichatservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatOptionsRequest(

    @NotBlank
    String message,

    @NotBlank
    String sessionId,

    Double temperature,      // override default (0.0 = deterministic, 1.0 = creative)

    Integer maxTokens      // limit response length
) {}