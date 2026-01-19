package com.scrumaiassistant.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiExtractionResult(
    @JsonProperty("meeting_id") String meetingId,
    @JsonProperty("project_key") String projectKey,
    List<String> decisions,
    List<Blocker> blockers,
    @JsonProperty("action_items") List<ActionItem> actionItems,
    List<JiraTask> tasks,
    List<Comment> comments,
    @JsonProperty("ticket_updates") List<TicketUpdate> ticketUpdates
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JiraTask(
        String title,
        String description,
        String assignee,
        String priority, // LOW, MEDIUM, HIGH
        @JsonProperty("due_date") String dueDate,
        List<String> labels,
        @JsonProperty("source_sentence") String sourceSentence
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Comment(
        String speaker,
        String comment,
        String type, // RISK|NOTE|CLARIFICATION|DEPENDENCY|APPROVAL|REJECTION|GENERAL
        @JsonProperty("related_ticket_keys") List<String> relatedTicketKeys,
        @JsonProperty("source_sentence") String sourceSentence
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TicketUpdate(
        @JsonProperty("ticket_key") String ticketKey,
        Updates updates,
        String reason,
        @JsonProperty("source_sentence") String sourceSentence,
        Double confidence
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Updates(
        String status,
        String assignee,
        String priority,
        @JsonProperty("due_date") String dueDate,
        @JsonProperty("labels_add") List<String> labelsAdd,
        @JsonProperty("labels_remove") List<String> labelsRemove,
        @JsonProperty("add_comment") String addComment,
        @JsonProperty("add_subtasks") List<ActionItem> addSubtasks // AccessItem structure matches subtask structure roughly?
        // Prompt says: add_subtasks: [{title, assignee, due_date, notes}]
        // ActionItem has: title, assignee, due_date, notes. So yes, we can reuse ActionItem or create SubTask record.
        // Let's reuse ActionItem for simplicity if it matches perfectly.
        // ActionItem: title, assignee, due_date, notes.
        // Subtask Schema: title, assignee, due_date, notes.
        // Match!
    ) {}
}
