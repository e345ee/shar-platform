package com.course.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    
    @NotBlank
    @Size(min = 32, message = "JWT secret must be at least 32 characters (256-bit) for HS256")
    private String secret;

    
    @Positive
    private long accessTokenTtlMinutes = 120;
}
