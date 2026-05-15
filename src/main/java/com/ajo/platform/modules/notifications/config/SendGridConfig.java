package com.ajo.platform.modules.notifications.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sendgrid")
public class SendGridConfig {

    private String apiKey;
    private String fromEmail;
    private String fromName;
}