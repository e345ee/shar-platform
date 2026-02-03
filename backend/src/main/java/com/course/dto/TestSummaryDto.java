package com.course.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TestSummaryDto {
    private Integer id;
    private Integer lessonId;
    private Integer courseId;

    private String title;
    private String description;
    private String topic;
    private LocalDateTime deadline;
    private String status;
    private LocalDateTime publishedAt;

    private Integer createdById;
    private String createdByName;

    private Integer questionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
