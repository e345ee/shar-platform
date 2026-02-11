package com.course.dto.statistics;

import lombok.Data;

@Data
public class StudentTopicStatsResponse {
    private Integer courseId;
    private String courseName;
    private String topic;

    
    private Long testsAttempted;
    
    private Long gradedTests;

    
    private Long attemptsCount;
    private Long gradedAttemptsCount;

    
    private Double avgBestPercent;

    

    public Integer getCourseId() {
        return this.courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return this.courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Long getTestsAttempted() {
        return this.testsAttempted;
    }

    public void setTestsAttempted(Long testsAttempted) {
        this.testsAttempted = testsAttempted;
    }

    public Long getGradedTests() {
        return this.gradedTests;
    }

    public void setGradedTests(Long gradedTests) {
        this.gradedTests = gradedTests;
    }

    public Long getAttemptsCount() {
        return this.attemptsCount;
    }

    public void setAttemptsCount(Long attemptsCount) {
        this.attemptsCount = attemptsCount;
    }

    public Long getGradedAttemptsCount() {
        return this.gradedAttemptsCount;
    }

    public void setGradedAttemptsCount(Long gradedAttemptsCount) {
        this.gradedAttemptsCount = gradedAttemptsCount;
    }

    public Double getAvgBestPercent() {
        return this.avgBestPercent;
    }

    public void setAvgBestPercent(Double avgBestPercent) {
        this.avgBestPercent = avgBestPercent;
    }

}
