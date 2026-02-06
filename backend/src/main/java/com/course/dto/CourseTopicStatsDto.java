package com.course.dto;

import lombok.Data;

@Data
public class CourseTopicStatsDto {
    private Integer courseId;
    private String courseName;
    private String topic;

    
    private Long studentsTotal;
    
    private Long studentsWithActivity;

    private Double avgPercent;
    private Long testsAttempted;
}
