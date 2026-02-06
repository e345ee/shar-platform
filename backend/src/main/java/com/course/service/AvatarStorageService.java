package com.course.service;

import com.course.config.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvatarStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final S3Client s3Client;
    private final S3Properties props;

    
    public String uploadAvatar(Integer userId, MultipartFile file) {
        validateAvatar(file);

        String contentType = file.getContentType();
        String ext = extensionForContentType(contentType);
        String key = "avatars/" + userId + "/" + UUID.randomUUID() + ext;

        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(req, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded file");
        }

        return buildPublicUrl(key);
    }

    
    public void deleteByPublicUrl(String publicUrl) {
        if (!StringUtils.hasText(publicUrl)) {
            return;
        }

        String prefix = props.getPublicUrl().replaceAll("/+$", "") + "/" + props.getBucket() + "/";
        if (!publicUrl.startsWith(prefix)) {
            
            return;
        }

        String key = publicUrl.substring(prefix.length());
        if (!StringUtils.hasText(key)) {
            return;
        }

        DeleteObjectRequest req = DeleteObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .build();
        s3Client.deleteObject(req);
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file is required");
        }

        long size = file.getSize();
        if (size <= 0) {
            throw new IllegalArgumentException("Avatar file is empty");
        }
        if (size > props.getMaxAvatarBytes()) {
            throw new IllegalArgumentException("Avatar file is too large (max " + props.getMaxAvatarBytes() + " bytes)");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Invalid avatar format. Allowed: JPEG, PNG, WEBP");
        }
    }

    private String extensionForContentType(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    private String buildPublicUrl(String key) {
        String base = props.getPublicUrl().replaceAll("/+$", "");
        return base + "/" + props.getBucket() + "/" + key;
    }
}
