package com.scrumaiassistant.model;

import com.scrumaiassistant.model.enums.TaskStatus;
import com.scrumaiassistant.model.enums.ToolType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    @ToString.Exclude
    private Meeting meeting;

    @Enumerated(EnumType.STRING)
    @Column(name = "tool_type")
    private ToolType toolType;

    @Column(name = "external_key_or_id")
    private String externalKeyOrId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String priority; // LOW, MEDIUM, HIGH

    private String assignee;

    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Column(name = "issue_type")
    private String issueType; // TASK, BUG, STORY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
