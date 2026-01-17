package com.scrumaiassistant.integration;

import com.scrumaiassistant.model.Task;

public interface IntegrationService {
    String createExternalTask(Task task);
    void updateTaskStatus(Task task);
    void sendNotification(String message);
}
