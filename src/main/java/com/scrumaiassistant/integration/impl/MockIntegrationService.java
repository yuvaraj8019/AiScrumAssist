package com.scrumaiassistant.integration.impl;

import com.scrumaiassistant.integration.IntegrationService;
import com.scrumaiassistant.model.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class MockIntegrationService implements IntegrationService {

    @Override
    public String createExternalTask(Task task) {
        log.info("Creating task in {}: {}", task.getToolType(), task.getTitle());
        return "EXT-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public void updateTaskStatus(Task task) {
        log.info("Updating task status in {} for ID: {}", task.getToolType(), task.getExternalKeyOrId());
    }

    @Override
    public void sendNotification(String message) {
        log.info("Sending Slack notification: {}", message);
    }

    @Override
    public void addCommentToExternalTask(String externalKey, Task task, String commentBody) {
        log.info("Mock addComment on {} for task '{}': {}", externalKey, task.getTitle(), commentBody);
    }
}
