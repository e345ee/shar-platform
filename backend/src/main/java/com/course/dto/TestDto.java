package com.course.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TestDto {
    private Integer id;
    private Integer lessonId;
    private Integer courseId;

    private String activityType;
    private Integer weightMultiplier;
    private java.time.LocalDate assignedWeekStart;
    private Integer timeLimitSeconds;

    private String title;
    private String description;
    private String topic;
    private LocalDateTime deadline;

    
    private String status;
    private LocalDateTime publishedAt;

    private Integer createdById;
    private String createdByName;

    private Integer questionCount;
    private List<TestQuestionDto> questions;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
