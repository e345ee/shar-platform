package com.course.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LessonDto {
    private Integer id;
    private Integer courseId;

    /** 1-based ordering of lessons inside a course. */
    private Integer orderIndex;

    private String title;
    private String description;

    /** Public URL to the uploaded PDF (presentation). */
    private String presentationUrl;

    private Integer createdById;
    private String createdByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
