package com.scrumaiassistant.integration.tools;

import com.scrumaiassistant.integration.AgileToolClient;
import com.scrumaiassistant.model.enums.ToolType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class AzureClient implements AgileToolClient {

    @Override
    public ToolType getToolType() {
        return ToolType.AZURE;
    }

    @Override
    public String createTask(String projectKey, String title, String description) {
        // Implementation for Azure Boards REST API would go here
        // https://dev.azure.com/{organization}/{project}/_apis/wit/workitems/$Task?api-version=6.0
        
        log.info("Creating Azure Board Work Item in project {}: {}", projectKey, title);
        
        // Simulating success
        return "AZ-" + UUID.randomUUID().toString().substring(0, 5);
    }
}
