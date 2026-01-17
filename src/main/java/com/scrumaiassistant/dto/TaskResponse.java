package com.scrumaiassistant.dto;

import com.scrumaiassistant.model.enums.TaskStatus;
import com.scrumaiassistant.model.enums.ToolType;
import java.time.LocalDateTime;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    UUID meetingId,
    ToolType toolType,
    String externalKeyOrId,
    String title,
    TaskStatus status,
    LocalDateTime createdAt
) {}
