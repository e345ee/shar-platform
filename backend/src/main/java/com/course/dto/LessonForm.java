package com.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Multipart form used for both create and update.
 */
@Data
public class LessonForm {

    /** Optional; if null the lesson is appended to the end; if provided, reorders the lesson inside its course. */
    @Min(1)
    private Integer orderIndex;

    @NotBlank
    @Size(min = 1, max = 127)
    private String title;

    @Size(max = 2048)
    private String description;

    // Optional; validated in service if present (content-type, size, etc)
    private MultipartFile presentation;
}
