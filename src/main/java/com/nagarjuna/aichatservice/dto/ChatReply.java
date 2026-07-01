package com.nagarjuna.aichatservice.dto;

import lombok.Builder;

@Builder
public record ChatReply(
    String sessionId,
    String response,
    Long inputTokens,
    Long outputTokens,
    long timestamp
) {}