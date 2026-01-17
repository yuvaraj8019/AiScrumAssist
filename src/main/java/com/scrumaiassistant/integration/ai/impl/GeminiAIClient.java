package com.scrumaiassistant.integration.ai.impl;

import com.scrumaiassistant.config.AiProperties;
import com.scrumaiassistant.integration.ai.AIClient;
import com.scrumaiassistant.integration.ai.dto.GeminiRequest;
import com.scrumaiassistant.integration.ai.dto.GeminiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAIClient implements AIClient {

    private final AiProperties aiProperties;
    private final WebClient.Builder webClientBuilder;

    @Override
    public String getProviderName() {
        return "GEMINI";
    }

    @Override
    public String generate(String prompt) {
        AiProperties.GeminiConfig config = aiProperties.getGemini();
        String apiKey = config.getApiKey();
        
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("Gemini API Key not configured");
        }

        String url = config.getBaseUrl() + "/v1beta/models/" + config.getModel() + ":generateContent?key=" + apiKey;

        GeminiRequest request = new GeminiRequest(
            List.of(new GeminiRequest.Content(
                List.of(new GeminiRequest.Part(prompt))
            )),
            new GeminiRequest.GenerationConfig(
                aiProperties.getGlobal().getTemperature(),
                aiProperties.getGlobal().getMaxTokens()
            )
        );

        log.info("Sending request to Gemini: {}", url.replaceAll("key=[^&]*", "key=***"));

        int maxRetries = 5;
        long waitTime = 2000;
        for (int i = 0; i < maxRetries; i++) {
            try {
                GeminiResponse response = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .block();
    
                if (response != null && !response.candidates().isEmpty()) {
                     // Gemini output is directly the text
                     return response.candidates().get(0).content().parts().get(0).text();
                }
            } catch (Exception e) {
                if (e.getMessage().contains("429") && i < maxRetries - 1) {
                    log.warn("Gemini Rate Limit hit (429). Retrying in {} ms... (Attempt {}/{})", waitTime, i + 1, maxRetries);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ignored) {}
                    waitTime *= 2; // Exponential backoff
                    continue;
                }
                log.error("Gemini Request Failed", e);
                // If it is the last attempt, or not a 429, throw
                if (i == maxRetries - 1 || !e.getMessage().contains("429")) {
                     throw new RuntimeException("Gemini Request Failed: " + e.getMessage());
                }
            }
        }
        
        throw new RuntimeException("Empty response from Gemini");
    }
}
