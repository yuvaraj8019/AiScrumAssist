package com.scrumaiassistant.integration.ai.impl;

import com.scrumaiassistant.config.AiProperties;
import com.scrumaiassistant.integration.ai.AIClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class GroqAIClient extends OpenAIClient implements AIClient {

    private final AiProperties aiProperties;

    public GroqAIClient(AiProperties aiProperties, WebClient.Builder webClientBuilder) {
        super(aiProperties, webClientBuilder);
        this.aiProperties = aiProperties;
    }

    @Override
    public String getProviderName() {
        return "GROQ";
    }

    @Override
    public String generate(String prompt) {
        AiProperties.GroqConfig config = aiProperties.getGroq();
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new RuntimeException("Groq API Key not configured");
        }

        return callOpenAICompatibleApi(
            config.getBaseUrl(),
            config.getApiKey(),
            config.getModel(),
            prompt
        );
    }
}
