package com.course.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PendingTestAttemptDto {
    private Integer attemptId;
    private Integer testId;
    private Integer lessonId;
    private Integer courseId;

    private Integer classId;
    private String className;

    private Integer studentId;
    private String studentName;

    private Integer ungradedOpenCount;

    private LocalDateTime submittedAt;
}
