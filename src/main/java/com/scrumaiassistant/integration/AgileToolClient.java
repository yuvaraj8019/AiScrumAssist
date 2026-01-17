package com.scrumaiassistant.integration;

import com.scrumaiassistant.model.enums.ToolType;

public interface AgileToolClient {
    /**
     * Creates a task in the external agile tool.
     * @param projectKey The project key/id in the external tool
     * @param title The title of the task
     * @param description The description of the task
     * @return The external ID/Key of the created task
     */
    String createTask(String projectKey, String title, String description);
    
    /**
     * Identifies which tool this client handles.
     * @return The ToolType enum (JIRA, AZURE)
     */
    ToolType getToolType();
}
