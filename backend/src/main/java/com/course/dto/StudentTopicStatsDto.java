package com.course.dto;

import lombok.Data;

@Data
public class StudentTopicStatsDto {
    private Integer courseId;
    private String courseName;
    private String topic;

    /** Distinct tests with this topic that the student has finished (SUBMITTED/GRADED). */
    private Long testsAttempted;
    /** Distinct tests with this topic where the best finished attempt is already GRADED. */
    private Long gradedTests;

    /** Total finished attempts (SUBMITTED/GRADED) inside this topic. */
    private Long attemptsCount;
    private Long gradedAttemptsCount;

    /** Average percent across distinct tests (best attempt per test). */
    private Double avgBestPercent;
}
