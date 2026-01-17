package com.scrumaiassistant.repository;

import com.scrumaiassistant.model.Task;
import com.scrumaiassistant.model.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByMeetingId(UUID meetingId);
    List<Task> findByCreatedAtAfterAndStatusNot(LocalDateTime createdAt, TaskStatus status);
}
