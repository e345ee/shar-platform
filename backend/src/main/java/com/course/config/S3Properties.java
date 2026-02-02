package com.course.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app.s3")
public class S3Properties {

    /** Example: http://minio:9000 */
    @NotBlank
    private String endpoint;

    /** Example: us-east-1 */
    @NotBlank
    private String region;

    @NotBlank
    private String accessKey;

    @NotBlank
    private String secretKey;

    /** Bucket where avatars are stored */
    @NotBlank
    private String bucket;

    /** Public base URL used to build the avatar link (usually same as endpoint) */
    @NotBlank
    private String publicUrl;

    /** Max avatar size in bytes (default 2MB) */
    private long maxAvatarBytes = 2L * 1024L * 1024L;
}
