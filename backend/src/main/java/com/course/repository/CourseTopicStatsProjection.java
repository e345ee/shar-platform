package com.course.repository;

public interface CourseTopicStatsProjection {
    Integer getCourseId();
    String getCourseName();
    String getTopic();

    Long getStudentsTotal();
    Long getStudentsWithActivity();
    Double getAvgPercent();
    Long getTestsAttempted();
}
