package com.course.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {
    
    private boolean enabled = false;

    
    private String from = "no-reply@course.local";
}
