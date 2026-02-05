package com.course.repository;

public interface TeacherStatsProjection {
    Integer getTeacherId();
    String getTeacherName();
    String getTeacherEmail();
    Long getClassesCount();
    Long getStudentsCount();
    Long getSubmittedAttemptsCount();
    Long getGradedAttemptsCount();
    Double getAvgGradePercent();
}
