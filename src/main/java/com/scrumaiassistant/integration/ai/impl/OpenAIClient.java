package com.scrumaiassistant.integration.ai.impl;

import com.scrumaiassistant.config.AiProperties;
import com.scrumaiassistant.integration.ai.AIClient;
import com.scrumaiassistant.integration.ai.dto.OpenAIRequest;
import com.scrumaiassistant.integration.ai.dto.OpenAIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIClient implements AIClient {

    private final AiProperties aiProperties;
    private final WebClient.Builder webClientBuilder;

    @Override
    public String getProviderName() {
        return "OPENAI";
    }

    @Override
    public String generate(String prompt) {
        AiProperties.OpenAiConfig config = aiProperties.getOpenai();
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            throw new RuntimeException("OpenAI API Key not configured");
        }

        return callOpenAICompatibleApi(
            config.getBaseUrl(),
            config.getApiKey(),
            config.getModel(),
            prompt
        );
    }
    
    protected String callOpenAICompatibleApi(String baseUrl, String apiKey, String model, String prompt) {
        String url = baseUrl + "/chat/completions";

        OpenAIRequest request = new OpenAIRequest(
            model,
            List.of(new OpenAIRequest.Message("user", prompt)),
            aiProperties.getGlobal().getTemperature(),
            aiProperties.getGlobal().getMaxTokens(),
            new OpenAIRequest.ResponseFormat("json_object") // Force JSON mode
        );

        log.info("Sending request to OpenAI/Compatible: {}", url);

        try {
            OpenAIResponse response = webClientBuilder.build()
                .post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAIResponse.class)
                .block();
    
            if (response != null && !response.choices().isEmpty()) {
                 return response.choices().get(0).message().content();
            }
        } catch (Exception e) {
            log.error("AI Request Failed", e);
            throw new RuntimeException("AI Request Failed: " + e.getMessage());
        }
        
        throw new RuntimeException("Empty response from AI Provider");
    }
}
