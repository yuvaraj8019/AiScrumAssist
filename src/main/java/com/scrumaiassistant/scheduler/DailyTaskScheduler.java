package com.scrumaiassistant.scheduler;

import com.scrumaiassistant.integration.IntegrationService;
import com.scrumaiassistant.model.Task;
import com.scrumaiassistant.model.enums.TaskStatus;
import com.scrumaiassistant.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyTaskScheduler {

    private final TaskRepository taskRepository;
    private final IntegrationService integrationService;

    // Runs daily at 9 AM
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkTaskStatus() {
        log.info("Running daily task status check");
        
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<Task> tasks = taskRepository.findByCreatedAtAfterAndStatusNot(yesterday, TaskStatus.COMPLETED);
        
        for (Task task : tasks) {
            // Check status update logic here
            // This is a placeholder as actual status check would involve calling Jira API
            integrationService.updateTaskStatus(task);
            
            // If incomplete, notify
            integrationService.sendNotification("Task incomplete: " + task.getTitle() + " (ID: " + task.getExternalKeyOrId() + ")");
        }
    }
}
