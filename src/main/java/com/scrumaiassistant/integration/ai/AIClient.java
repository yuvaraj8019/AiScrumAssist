package com.scrumaiassistant.integration.ai;

public interface AIClient {
    /**
     * Generates content from the AI provider.
     * @param prompt The prompt to send to the AI
     * @return The raw text response (typically JSON in our use case)
     */
    String generate(String prompt);
    
    /**
     * Returns the name of the provider.
     * @return Provider name (GEMINI, OPENAI, GROQ)
     */
    String getProviderName();
}
