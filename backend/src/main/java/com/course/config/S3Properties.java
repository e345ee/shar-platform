package com.course.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app.s3")
public class S3Properties {

    @NotBlank
    private String endpoint;

    @NotBlank
    private String region;

    @NotBlank
    private String accessKey;

    @NotBlank
    private String secretKey;

    @NotBlank
    private String bucket;

    @NotBlank
    private String publicUrl;

    private long maxAvatarBytes = 2L * 1024L * 1024L;
}
