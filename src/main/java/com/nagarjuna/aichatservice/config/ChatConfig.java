package com.nagarjuna.aichatservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ChatConfig {

    private final AppProperties appProperties;

    // Chat Memory: Stores convo in history
    @Bean
    public ChatMemory chatMemory() {

        return MessageWindowChatMemory
                .builder()
                .maxMessages(appProperties.getMemoryWindow())
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .build();
    }

    // ChatClient config for configuring the system prompt
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory memory) {

        String systemPrompt = buildSystemPrompt();

        // Advisor 1: Injects convo history as msgs
        // Order(1) - Runs first
        return builder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory)
                        .order(1)
                        .build()
                )
                // Advisor 2: Logger logs full request + response (DEBUG level)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                )
                .build();
    }

    private String buildSystemPrompt() {

        try {

            PromptTemplate template =
                    new PromptTemplate(
                            new ClassPathResource(
                                    "/prompts/system.st"
                            )
                    );

            return template
                    .render(
                            Map.of(
                                    "date",
                                    LocalDate.now().toString()
                            )
                    );

        } catch (Exception e) {

            log.info("System prompt file not found, using default");

            return "You are an helpful assistant named Aria. Today is " + LocalDate.now();
        }
    }
}
