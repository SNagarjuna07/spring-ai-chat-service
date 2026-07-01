package com.nagarjuna.aichatservice.dto;

import jakarta.validation.constraints.NotBlank;

public record EntityRequest(

        @NotBlank
        String topic
) {}