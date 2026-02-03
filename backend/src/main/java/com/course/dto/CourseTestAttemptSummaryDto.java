package com.course.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseTestAttemptSummaryDto {
    private Integer attemptId;
    private Integer testId;
    private Integer lessonId;
    private Integer courseId;

    private Integer studentId;
    private String studentName;

    private Integer attemptNumber;
    private String status;
    private Integer score;
    private Integer maxScore;
    private Double percent;

    private Integer weightedScore;
    private Integer weightedMaxScore;
    private Double weightedPercent;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
}
