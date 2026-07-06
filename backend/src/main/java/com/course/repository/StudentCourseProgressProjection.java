package com.course.repository;

public interface StudentCourseProgressProjection {
    Integer getCourseId();
    String getCourseName();
    Long getRequiredTests();
    Long getCompletedTests();
    Double getPercent();
    Boolean getCompleted();
}
