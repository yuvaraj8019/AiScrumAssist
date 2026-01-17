package com.scrumaiassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jira")
@Data
public class JiraProperties {
    private String url;
    private String email;
    private String apiToken;
}
