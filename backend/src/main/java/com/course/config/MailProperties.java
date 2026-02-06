package com.course.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lightweight app-level mail configuration.
 *
 * We intentionally keep SMTP connection details in standard Spring properties
 * (spring.mail.*) and only store "enabled" and "from" here.
 */
@Data
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {
    /**
     * When disabled, email sending endpoints will return a 503.
     */
    private boolean enabled = false;

    /**
     * Default From address.
     */
    private String from = "no-reply@course.local";
}
