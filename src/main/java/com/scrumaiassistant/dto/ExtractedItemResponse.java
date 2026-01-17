package com.scrumaiassistant.dto;

import com.scrumaiassistant.model.enums.ItemType;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExtractedItemResponse(
    UUID id,
    UUID meetingId,
    ItemType itemType,
    String content,
    LocalDateTime createdAt
) {}
