package com.course.dto;

import lombok.Data;

/**
 * Methodist-level teacher statistics summary (SRS 3.1.3/3.1.4).
 */
@Data
public class TeacherStatsDto {
    private Integer teacherId;
    private String teacherName;
    private String teacherEmail;
    private Long classesCount;
    private Long studentsCount;
    private Long submittedAttemptsCount;
    private Long gradedAttemptsCount;
    /**
     * Average percent for graded attempts (0..100). Can be null if there are no graded attempts.
     */
    private Double avgGradePercent;
}
