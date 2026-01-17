package com.scrumaiassistant.integration.ai.dto;

import java.util.List;

public record OpenAIResponse(
    List<Choice> choices
) {
    public record Choice(Message message) {}
    public record Message(String content) {}
}
