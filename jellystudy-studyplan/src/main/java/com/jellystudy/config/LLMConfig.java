package com.jellystudy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LLMConfig {
    private String provider = "minimax";
    private String apiKey;
    private String baseUrl;
    private String model;
    private int timeout = 30000;
    private double temperature = 0.7;
    private int maxTokens = 1024;
}
