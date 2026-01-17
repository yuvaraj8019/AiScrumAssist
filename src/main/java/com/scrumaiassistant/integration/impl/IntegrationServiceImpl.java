package com.scrumaiassistant.integration.impl;

import com.scrumaiassistant.integration.AgileToolClient;
import com.scrumaiassistant.integration.IntegrationService;
import com.scrumaiassistant.model.Task;
import com.scrumaiassistant.model.enums.ToolType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Primary
@Slf4j
public class IntegrationServiceImpl implements IntegrationService {

    private final Map<ToolType, AgileToolClient> clientMap;

    public IntegrationServiceImpl(List<AgileToolClient> clients) {
        this.clientMap = clients.stream()
                .collect(Collectors.toMap(AgileToolClient::getToolType, Function.identity()));
    }

    @Override
    public String createExternalTask(Task task) {
        AgileToolClient client = clientMap.get(task.getToolType());
        
        if (client == null) {
            log.warn("No client found for tool type: {}", task.getToolType());
            return "ERR-NO-CLIENT";
        }

        String projectKey = task.getMeeting().getProjectKey();
        if (projectKey == null || projectKey.isEmpty()) {
            projectKey = "KAN"; // Default or throw error
        }

        try {
            return client.createTask(projectKey, task.getTitle(), "Created by AI Scrum Assistant via Meeting ID: " + task.getMeeting().getId());
        } catch (Exception e) {
             log.error("Integration failed for {}", task.getToolType(), e);
             return "FAIL-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    @Override
    public void updateTaskStatus(Task task) {
        log.info("Updating task status in {} for ID: {}", task.getToolType(), task.getExternalKeyOrId());
    }

    @Override
    public void sendNotification(String message) {
        log.info("Sending Slack notification: {}", message);
    }
}
