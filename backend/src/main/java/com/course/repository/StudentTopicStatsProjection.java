package com.course.repository;

public interface StudentTopicStatsProjection {
    Integer getCourseId();
    String getCourseName();
    String getTopic();

    Long getTestsAttempted();
    Long getGradedTests();
    Long getAttemptsCount();
    Long getGradedAttemptsCount();
    Double getAvgBestPercent();
}
