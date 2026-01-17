package com.scrumaiassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai")
@Data
public class AiProperties {
    private String provider;
    private GlobalConfig global = new GlobalConfig();
    private GeminiConfig gemini = new GeminiConfig();
    private OpenAiConfig openai = new OpenAiConfig();
    private GroqConfig groq = new GroqConfig();

    @Data
    public static class GlobalConfig {
        private Double temperature;
        private Integer maxTokens;
    }

    @Data
    public static class GeminiConfig {
        private String apiKey;
        private String baseUrl;
        private String model;
    }

    @Data
    public static class OpenAiConfig {
        private String apiKey;
        private String baseUrl;
        private String model;
    }

    @Data
    public static class GroqConfig {
        private String apiKey;
        private String baseUrl;
        private String model;
    }
}
