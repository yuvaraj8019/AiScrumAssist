package com.scrumaiassistant.integration.ai.dto;

import java.util.List;

public record OpenAIRequest(
    String model,
    List<Message> messages,
    double temperature,
    int max_tokens,
    ResponseFormat response_format
) {
    public record Message(String role, String content) {}
    public record ResponseFormat(String type) {}
}
