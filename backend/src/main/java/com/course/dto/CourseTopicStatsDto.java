package com.course.dto;

import lombok.Data;

@Data
public class CourseTopicStatsDto {
    private Integer courseId;
    private String courseName;
    private String topic;

    /** Total students enrolled in the course (distinct). */
    private Long studentsTotal;
    /** Students with at least one finished attempt in this topic. */
    private Long studentsWithActivity;

    private Double avgPercent;
    private Long testsAttempted;
}
