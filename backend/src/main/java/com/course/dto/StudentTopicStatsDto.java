package com.course.dto;

import lombok.Data;

@Data
public class StudentTopicStatsDto {
    private Integer courseId;
    private String courseName;
    private String topic;

    
    private Long testsAttempted;
    
    private Long gradedTests;

    
    private Long attemptsCount;
    private Long gradedAttemptsCount;

    
    private Double avgBestPercent;
}
