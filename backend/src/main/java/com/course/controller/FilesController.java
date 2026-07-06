package com.course.controller;

import com.course.config.S3Properties;
import com.course.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FilesController {

    private final S3Client s3Client;
    private final S3Properties props;

    @GetMapping("/{*key}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> get(@PathVariable("key") String key) {
        if (!StringUtils.hasText(key)) {
            throw new ResourceNotFoundException("File not found");
        }

        try {
            GetObjectRequest req = GetObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> objectStream = s3Client.getObject(req);
            GetObjectResponse meta = objectStream.response();

            MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
            String metaType = meta.contentType();
            if (StringUtils.hasText(metaType)) {
                try {
                    contentType = MediaType.parseMediaType(metaType);
                } catch (IllegalArgumentException ignored) {
                    contentType = MediaType.APPLICATION_OCTET_STREAM;
                }
            }

            ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                    .contentType(contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600");

            Long length = meta.contentLength();
            if (length != null && length > 0) {
                builder.contentLength(length);
            }

            return builder.body(new InputStreamResource(objectStream));
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("File not found");
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new ResourceNotFoundException("File not found");
            }
            throw e;
        }
    }
}
