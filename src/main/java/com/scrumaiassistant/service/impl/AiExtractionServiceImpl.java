package com.scrumaiassistant.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrumaiassistant.dto.ai.AiExtractionResult;
import com.scrumaiassistant.integration.ai.AIClient;
import com.scrumaiassistant.integration.ai.AIClientFactory;
import com.scrumaiassistant.service.AiExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class AiExtractionServiceImpl implements AiExtractionService {

    private final AIClientFactory aiClientFactory;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            Extract Scrum data from the transcript below into strict JSON.
            Use this schema:
            {
              "decisions": ["string"],
              "blockers": [{"description": "string", "owner": "string", "impact": "string"}],
              "action_items": [{"title": "string", "assignee": "string", "due_date": "YYYY-MM-DD", "notes": "string"}],
              "tasks": [{"title": "string", "description": "string", "assignee": "string", "priority": "LOW|MEDIUM|HIGH", "due_date": "YYYY-MM-DD", "labels": ["string"], "source_sentence": "string"}]
            }
            
            Transcript:
            %s
            """;

    @Override
    public AiExtractionResult extractInfo(String transcript) {
        // We need meeting ID and Project Key. 
        // NOTE: The current interface only accepts transcript string. 
        // Ideally, we should refactor the interface to accept Meeting object or ID/ProjectKey.
        // For now, avoiding interface breakage, we'll strip them or use placeholders in prompt 
        // but since the extraction result needs to map back, typically the Caller handles ID.
        // The prompt asks for ID/KEY input, so let's try to extract if embedded or pass dummies.
        // Actually, the caller (MeetingServiceImpl) has the context. 
        // Let's overload or assume extraction result validates it.
        
        return extractInfo("UNKNOWN", "UNKNOWN", transcript);
    }
    
    // New overloaded method (to be added to interface, or casted usage)
    public AiExtractionResult extractInfo(String meetingId, String projectKey, String transcript) {
        AIClient client = aiClientFactory.getClient();
        log.info("Using AI Provider: {}", client.getProviderName());
        
        String prompt = String.format(SYSTEM_PROMPT, quote(transcript));
        
        String rawResponse = client.generate(prompt);
        
        try {
            return parseJson(rawResponse);
        } catch (Exception e) {
            log.warn("First extraction failed due to invalid JSON. Retrying with strict instruction...");
            // Retry logic
            String retryPrompt = prompt + "\n\nIMPORTANT: The previous output was invalid JSON. Please correct it and return ONLY raw JSON.";
            String retryResponse = client.generate(retryPrompt);
            try {
                return parseJson(retryResponse);
            } catch (Exception ex) {
                log.error("Failed to parse AI response after retry. Raw: {}", retryResponse);
                throw new RuntimeException("AI Extraction failed to produce valid JSON");
            }
        }
    }
    
    private AiExtractionResult parseJson(String text) throws JsonProcessingException {
        // Strip markdown code blocks if present (common LLM behavior)
        if (text.contains("```json")) {
            text = text.replace("```json", "").replace("```", "");
        } else if (text.contains("```")) {
            text = text.replace("```", "");
        }
        
        return objectMapper.readValue(text.trim(), AiExtractionResult.class);
    }
    
    // Helper to escape transcript for JSON embedding effectively
    private String quote(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\n", " ");
    }
}
