package com.scrumaiassistant.service;

import com.scrumaiassistant.dto.ai.AiExtractionResult;

public interface AiExtractionService {
    AiExtractionResult extractInfo(String transcript);
    AiExtractionResult extractInfo(String meetingId, String projectKey, String transcript);
}
