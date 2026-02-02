package com.course.service;

import com.course.config.S3Properties;
import com.course.exception.AchievementPhotoValidationException;
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
public class AchievementPhotoStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final S3Client s3Client;
    private final S3Properties props;

    public String uploadAchievementPhoto(Integer courseId, MultipartFile file) {
        validatePhoto(file);

        String contentType = file.getContentType();
        String ext = extensionForContentType(contentType);
        String key = "achievements/course-" + courseId + "/" + UUID.randomUUID() + ext;

        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(req, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new AchievementPhotoValidationException("Failed to read uploaded file");
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

    private void validatePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AchievementPhotoValidationException("Achievement photo is required");
        }

        long size = file.getSize();
        if (size <= 0) {
            throw new AchievementPhotoValidationException("Achievement photo file is empty");
        }

        long max = props.getMaxAchievementPhotoBytes() > 0 ? props.getMaxAchievementPhotoBytes() : props.getMaxAvatarBytes();
        if (size > max) {
            throw new AchievementPhotoValidationException("Achievement photo is too large (max " + max + " bytes)");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new AchievementPhotoValidationException("Invalid achievement photo format. Allowed: JPEG, PNG, WEBP");
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
