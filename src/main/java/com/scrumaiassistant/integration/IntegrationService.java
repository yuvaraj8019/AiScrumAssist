package com.scrumaiassistant.integration;

import com.scrumaiassistant.model.Task;

public interface IntegrationService {
    String createExternalTask(Task task);
    void updateTaskStatus(Task task);
    void sendNotification(String message);
    /**
     * Posts a comment body on the given external issue key (e.g. KAN-42).
     */
    void addCommentToExternalTask(String externalKey, Task task, String commentBody);
}

