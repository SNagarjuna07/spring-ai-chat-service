package com.nagarjuna.aichatservice.service;

import com.nagarjuna.aichatservice.dto.ChatOptionsRequest;
import com.nagarjuna.aichatservice.entity.TopicSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatClient chatClient;

    private final ChatMemory chatMemory;

    // --- 1. Basic blocking call ---
    // ChatClient.prompt()  -> start building prompt
    // .user(msg)           -> set user message
    // .advisors(...)       -> set CONVERSATION_ID (2.0.0-M6 API - mandatory)
    // .call()              -> execute (blocking)
    // .content()           -> extract String response
    public String chat(String sessionId, String userMessage) {

        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .content();
    }

    // --- 2. Full ChatResponse (with token metadata) ---
    // .chatResponse() returns full ChatResponse object instead of just the text string.
    // Gives you: token usage, model name, finish reason, etc.
    public Map<String, Object> chatWithMetadata(String sessionId, String userMessage) {

        ChatResponse response = chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .chatResponse();

        // Extract text
        String text = response
                .getResult()
                .getOutput()
                .getText();

        // Extract usage metrics
        Usage usage = response
                .getMetadata()
                .getUsage();

        Integer inputTokens = Optional.ofNullable(usage)
                .map(Usage::getPromptTokens).orElse(null);

        Integer outputTokens = Optional.ofNullable(usage)
                .map(Usage::getCompletionTokens).orElse(null);

        String model = response
                .getMetadata()
                .getModel();

        log.info("Token usage - input: {}, output: {}", inputTokens, outputTokens);

        return Map.of(
                "sessionId", sessionId,
                "response", text,
                "inputTokens", inputTokens != null ? inputTokens : "N/A",
                "outputTokens", outputTokens != null ? outputTokens : "N/A",
                "model", model != null ? response.getMetadata().getModel() : "Unknown"
        );
    }

    // --- 3. Streaming (reactive Flux) ---
    // .stream() instead of .call()
    // .content() returns Flux<String> - one item per token
    // Perfect for SSE to frontend (feels like ChatGPT)
    public Flux<String> chatStream(String sessionId, String userMessage) {

        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .content();
    }

    // --- 4: Per-call ChatOptions override ---
    // Override model defaults for specific requests.
    // E.g. creative writing -> high temperature
    // factual Q&A -> temperature 0
    public String chatWithOptions(ChatOptionsRequest req) {

        return chatClient.prompt()
                .user(req.message())
                .options(OpenAiChatOptions.builder()
                        .temperature(req.temperature() != null ? req.temperature() : 0.7)
                        .maxCompletionTokens(req.maxTokens() != null ? req.maxTokens() : 500)
                )
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, req.sessionId()))
                .call()
                .content();
    }

    // --- 5. Structured output (.entity()) ---
    // Spring AI injects JSON schema of your class into the prompt,
    // tells LLM to respond in that format, then deserializes
    // JSON -> Java object automatically.
    // Use for: data extraction, classification, structured generation.
    public TopicSummary chatStructured(String topic) {

        return chatClient.prompt()
                .user(u -> u
                        .text("Generate a concise summary of the topic: {topic}. " +
                                "Include a title, a 2-sentence summary, and 3 key points.")
                        .param("topic", topic)
                )
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID,
                        "structured-" + java.util.UUID.randomUUID()
                ))
                .call()
                .entity(TopicSummary.class);
    }

    // --- 6. PromptTemplate with inline params ---
    // Using .text(...).param(...) is inline prompt templating.
    // Variables in {curly} braces get substituted before sending to LLM.
    public String translateText(String text, String targetLanguage) {

        return chatClient.prompt()
                .user(u -> u
                        .text("Translate the following text to {language}:\n\n{text}")
                        .param("language", targetLanguage)
                        .param("text", text)
                )
                .call()
                .content();
    }

    // Clear session memory
    public void clearSession(String sessionId) {

        chatMemory.clear(sessionId);

        log.info("Cleared memory for session: {}", sessionId);
    }
}