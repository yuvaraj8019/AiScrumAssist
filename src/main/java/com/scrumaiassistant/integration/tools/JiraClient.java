package com.scrumaiassistant.integration.tools;

import com.scrumaiassistant.config.JiraProperties;
import com.scrumaiassistant.integration.AgileToolClient;
import com.scrumaiassistant.model.enums.ToolType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JiraClient implements AgileToolClient {

    private final JiraProperties jiraProperties;
    private final WebClient.Builder webClientBuilder;

    private WebClient webClient;

    private void init() {
        if (webClient == null) {
            String email = jiraProperties.getEmail();
            if (email == null || email.contains("REPLACE")) {
                log.warn("Jira Email is not set correctly. Calls might fail.");
                email = "dummy"; // Prevent NPE
            }

            String auth = email + ":" + jiraProperties.getApiToken();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            webClient = webClientBuilder
                    .baseUrl(jiraProperties.getUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            log.info("JiraClient initialized with Email: {} (Length: {}), Token Length: {}",
                     email != null ? email.replaceAll("(?<=.{2}).(?=.*@)", "*") : "null",
                     email != null ? email.length() : 0,
                     jiraProperties.getApiToken() != null ? jiraProperties.getApiToken().length() : 0);
        }
    }

    @Override
    public ToolType getToolType() {
        return ToolType.JIRA;
    }

    @Override
    public String createTask(String projectKey, String title, String description) {
        init();
        log.info("Creating Jira Task in project {} with title {}", projectKey, title);

        try {
            Map<String, Object> payload = Map.of(
                "fields", Map.of(
                    "project", Map.of("key", projectKey),
                    "summary", title,
                    "description", description != null ? description : "",
                    "issuetype", Map.of("name", "Task")
                )
            );

            Map response = webClient.post()
                    .uri("/rest/api/2/issue")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("key")) {
                String key = (String) response.get("key");
                log.info("Successfully created Jira issue: {}", key);
                return key;
            }

        } catch (Exception e) {
            log.error("Failed to create Jira task", e);
            throw new RuntimeException("Jira creation failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Adds a comment to an existing Jira issue using the REST API v2 comment endpoint.
     * Non-fatal: logs errors but does not propagate exceptions to avoid breaking the pipeline.
     */
    @Override
    public void addComment(String issueKey, String comment) {
        init();
        if (issueKey == null || issueKey.isBlank() || comment == null || comment.isBlank()) {
            log.warn("Skipping addComment: issueKey or comment is blank. issueKey={}", issueKey);
            return;
        }
        log.info("Adding comment to Jira issue: {}", issueKey);
        try {
            Map<String, Object> payload = Map.of("body", comment);

            webClient.post()
                    .uri("/rest/api/2/issue/{issueKey}/comment", issueKey)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("Successfully added comment to Jira issue: {}", issueKey);
        } catch (Exception e) {
            log.error("Failed to add comment to Jira issue: {}", issueKey, e);
            // Non-fatal: log but don't fail the whole pipeline
        }
    }
}
