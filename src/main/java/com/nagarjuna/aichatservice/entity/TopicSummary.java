package com.nagarjuna.aichatservice.entity;

import java.util.List;

// Spring AI uses this class's structure to generate JSON schema
// that gets injected into the LLM prompt automatically
public record TopicSummary(

        String title,

        String summary,       // 2-sentence summary

        List<String> keyPoints  // exactly 3 key points
) {}