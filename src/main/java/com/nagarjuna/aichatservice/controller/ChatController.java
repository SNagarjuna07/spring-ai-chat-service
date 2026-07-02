package com.nagarjuna.aichatservice.controller;

import com.nagarjuna.aichatservice.dto.ChatOptionsRequest;
import com.nagarjuna.aichatservice.dto.ChatReply;
import com.nagarjuna.aichatservice.dto.ChatRequest;
import com.nagarjuna.aichatservice.dto.EntityRequest;
import com.nagarjuna.aichatservice.entity.TopicSummary;
import com.nagarjuna.aichatservice.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ChatController {

    private final ChatService chatService;

    // Basic chat - returns String response
    @PostMapping
    public ResponseEntity<ChatReply> chat(
            @Valid @RequestBody
            ChatRequest req
    ) {

        log.info("Chat request | session ID: {}", req.sessionId());

        String response = chatService.chat(req.sessionId(), req.message());

        return ResponseEntity.ok(
                new ChatReply(
                        req.sessionId(),
                        response,
                        null,
                        null,
                        System.currentTimeMillis()
                )
        );
    }

    // Returns response + token usage info
    @PostMapping("/metadata")
    public ResponseEntity<Map<String, Object>> chatWithMetadata(
            @Valid @RequestBody ChatRequest req) {

        return ResponseEntity.ok(
                chatService.chatWithMetadata(req.sessionId(), req.message())
        );
    }

    // Streaming SSE - tokens arrive one by one
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(
            @RequestParam String sessionId,
            @RequestParam String message) {

        log.info("Stream | session={}", sessionId);

        return chatService.chatStream(sessionId, message);
    }

    @PostMapping("/options")
    public ResponseEntity<Map<String, String>> chatWithOptions(
            @Valid @RequestBody ChatOptionsRequest req) {

        String response = chatService.chatWithOptions(req);
        return ResponseEntity.ok(Map.of(
                "sessionId", req.sessionId(),
                "response", response,
                "temperature", String.valueOf(req.temperature())
        ));
    }

    // Structured output - returns typed Java object
    @PostMapping("/structured")
    public ResponseEntity<TopicSummary> structured(
            @Valid @RequestBody EntityRequest req) {

        TopicSummary summary = chatService.chatStructured(req.topic());
        return ResponseEntity.ok(summary);
    }

    // Prompt template with params demo
    @PostMapping("/translate")
    public ResponseEntity<Map<String, String>> translate(
            @RequestParam String text,
            @RequestParam String language) {

        String translated = chatService.translateText(text, language);

        return ResponseEntity.ok(
                Map.of(
                        "original", text,
                        "language", language,
                        "translated", translated
                ));
    }

    // Clear session memory (new conversation)
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, String>> clearSession(
            @PathVariable String sessionId) {

        chatService.clearSession(sessionId);
        return ResponseEntity.ok(Map.of(
                "message", "Session cleared",
                "sessionId", sessionId
        ));
    }
}