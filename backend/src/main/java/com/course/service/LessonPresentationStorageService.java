package com.course.service;

import com.course.config.S3Properties;
import com.course.exception.LessonPresentationNotFoundException;
import com.course.exception.LessonPresentationValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonPresentationStorageService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final S3Client s3Client;
    private final S3Properties props;

    public String uploadPresentation(Integer courseId, MultipartFile file) {
        validatePdf(file);

        String key = "lessons/course-" + courseId + "/presentations/" + UUID.randomUUID() + ".pdf";

        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .contentType(PDF_CONTENT_TYPE)
                    .build();

            s3Client.putObject(req, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new LessonPresentationValidationException("Failed to read uploaded PDF");
        }

        return buildPublicUrl(key);
    }

    public void deleteByPublicUrl(String publicUrl) {
        if (!StringUtils.hasText(publicUrl)) {
            return;
        }

        String key = extractKeyFromPublicUrlOrNull(publicUrl);
        if (!StringUtils.hasText(key)) {
            return;
        }

        DeleteObjectRequest req = DeleteObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .build();
        s3Client.deleteObject(req);
    }

    public byte[] downloadByPublicUrl(String publicUrl) {
        if (!StringUtils.hasText(publicUrl)) {
            throw new LessonPresentationNotFoundException("Lesson presentation not found");
        }

        String key = extractKeyFromPublicUrlOrNull(publicUrl);
        if (!StringUtils.hasText(key)) {
            throw new LessonPresentationNotFoundException("Lesson presentation not found");
        }

        try {
            GetObjectRequest req = GetObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .build();

            return s3Client.getObject(req, ResponseTransformer.toBytes()).asByteArray();
        } catch (S3Exception e) {
            // MinIO often returns 404 for missing keys.
            if (e.statusCode() == 404) {
                throw new LessonPresentationNotFoundException("Lesson presentation not found");
            }
            throw new LessonPresentationValidationException("Failed to download lesson presentation");
        }
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new LessonPresentationValidationException("PDF presentation file is required");
        }

        long size = file.getSize();
        if (size <= 0) {
            throw new LessonPresentationValidationException("PDF presentation file is empty");
        }

        long max = props.getMaxLessonPdfBytes();
        if (max > 0 && size > max) {
            throw new LessonPresentationValidationException("PDF presentation is too large (max " + max + " bytes)");
        }

        String contentType = file.getContentType();
        String original = file.getOriginalFilename();

        boolean isPdfByType = StringUtils.hasText(contentType) && PDF_CONTENT_TYPE.equalsIgnoreCase(contentType);
        boolean isPdfByName = StringUtils.hasText(original) && original.toLowerCase().endsWith(".pdf");
        if (!isPdfByType && !isPdfByName) {
            throw new LessonPresentationValidationException("Invalid presentation format. Only PDF is allowed");
        }
    }

    private String buildPublicUrl(String key) {
        String base = props.getPublicUrl().replaceAll("/+$", "");
        return base + "/" + props.getBucket() + "/" + key;
    }

    private String extractKeyFromPublicUrlOrNull(String publicUrl) {
        if (!StringUtils.hasText(publicUrl)) {
            return null;
        }

        String prefix = props.getPublicUrl().replaceAll("/+$", "") + "/" + props.getBucket() + "/";
        if (!publicUrl.startsWith(prefix)) {
            return null;
        }

        String key = publicUrl.substring(prefix.length());
        return StringUtils.hasText(key) ? key : null;
    }
}
