package com.course.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TestAttemptDto {
    private Integer id;

    private Integer testId;
    private Integer lessonId;
    private Integer courseId;

    private Integer studentId;
    private String studentName;

    private Integer attemptNumber;
    private String status;

    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    private Integer score;
    private Integer maxScore;
    private Double percent;

    private Integer weightedScore;
    private Integer weightedMaxScore;
    private Double weightedPercent;

    private List<TestAttemptAnswerDto> answers;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
