package com.scrumaiassistant.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiExtractionResult(
    String meeting_id,
    String project_key,
    List<String> decisions,
    List<Blocker> blockers,
    @JsonProperty("action_items") List<ActionItem> actionItems,
    List<JiraTask> tasks
) {
    public record Blocker(
        String description, 
        String owner,
        String impact
    ) {}
    
    public record ActionItem(
         String title,
         String assignee,
         @JsonProperty("due_date") String dueDate,
         String notes
    ) {}

    public record JiraTask(
        String title,
        String description,
        String assignee,
        String priority, // LOW, MEDIUM, HIGH
        @JsonProperty("due_date") String dueDate,
        List<String> labels,
        @JsonProperty("source_sentence") String sourceSentence
    ) {}
}
