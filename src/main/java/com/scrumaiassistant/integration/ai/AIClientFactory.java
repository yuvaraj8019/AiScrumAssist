package com.scrumaiassistant.integration.ai;

import com.scrumaiassistant.config.AiProperties;
import com.scrumaiassistant.integration.ai.impl.GeminiAIClient;
import com.scrumaiassistant.integration.ai.impl.GroqAIClient;
import com.scrumaiassistant.integration.ai.impl.OpenAIClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AIClientFactory {

    private final AiProperties aiProperties;
    private final GeminiAIClient geminiClient;
    private final OpenAIClient openAIClient;
    private final GroqAIClient groqClient;

    public AIClient getClient() {
        String provider = aiProperties.getProvider();

        if ("AUTO".equalsIgnoreCase(provider)) {
            if (isValid(aiProperties.getGemini().getApiKey())) {
                log.info("Auto-selected AI Provider: GEMINI");
                return geminiClient;
            } else if (isValid(aiProperties.getOpenai().getApiKey())) {
                log.info("Auto-selected AI Provider: OPENAI");
                return openAIClient;
            } else if (isValid(aiProperties.getGroq().getApiKey())) {
                log.info("Auto-selected AI Provider: GROQ");
                return groqClient;
            } else {
                throw new RuntimeException("No AI provider configured. Please set AI_PROVIDER or ensure an API Key is set in .env");
            }
        }

        return switch (provider.toUpperCase()) {
            case "GEMINI" -> geminiClient;
            case "OPENAI" -> openAIClient;
            case "GROQ" -> groqClient;
            default -> throw new RuntimeException("Unknown AI Provider: " + provider);
        };
    }

    private boolean isValid(String key) {
        return key != null && !key.isEmpty();
    }
}
