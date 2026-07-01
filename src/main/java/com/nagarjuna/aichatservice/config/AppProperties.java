package com.nagarjuna.aichatservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "app.chat")
@Component
@Data
public class AppProperties {

    private String systemPrompt;

    private int memoryWindow = 10;
}
