package com.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestAttemptAdminSummaryDto {
    private Integer id;
    private Integer testId;
    private Integer studentId;
    private String studentName;
    private Integer attemptNo;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Boolean isLate;
    private Integer score;
    private Integer maxScore;
}
