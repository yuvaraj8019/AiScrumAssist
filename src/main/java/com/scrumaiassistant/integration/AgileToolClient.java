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
     * Adds a comment to an existing issue in the external agile tool.
     * @param issueKey The external issue key (e.g. KAN-42)
     * @param comment  The comment body text
     */
    void addComment(String issueKey, String comment);

    /**
     * Identifies which tool this client handles.
     * @return The ToolType enum (JIRA, AZURE)
     */
    ToolType getToolType();
}
