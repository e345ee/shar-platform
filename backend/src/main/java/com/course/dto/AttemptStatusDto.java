package com.course.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttemptStatusDto {
    private Integer testId;
    private Integer attemptId;
    private String status;
    private Integer score;
    private Integer maxScore;
    private Integer weightedScore;
    private Integer weightedMaxScore;
    private LocalDateTime submittedAt;
}
