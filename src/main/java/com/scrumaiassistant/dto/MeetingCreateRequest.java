package com.scrumaiassistant.dto;

import com.scrumaiassistant.model.enums.CeremonyType;
import com.scrumaiassistant.model.enums.ToolType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record MeetingCreateRequest(
    @NotBlank(message = "Title is required")
    String title,
    
    @NotNull(message = "Ceremony Type is required")
    CeremonyType ceremonyType,
    
    LocalDateTime meetingDate,
    
    @NotNull(message = "Tool Type is required")
    ToolType toolType,
    
    @NotBlank(message = "Project Key is required")
    String projectKey
) {}
