package com.scrumaiassistant.dto;

import com.scrumaiassistant.model.enums.CeremonyType;
import com.scrumaiassistant.model.enums.MeetingStatus;
import com.scrumaiassistant.model.enums.ToolType;
import java.time.LocalDateTime;
import java.util.UUID;

public record MeetingResponse(
    UUID id,
    String title,
    CeremonyType ceremonyType,
    LocalDateTime meetingDate,
    ToolType toolType,
    String projectKey,
    MeetingStatus status,
    String transcript,
    String summary,
    String audioFilename,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
