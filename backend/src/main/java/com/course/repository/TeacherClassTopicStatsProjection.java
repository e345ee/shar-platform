package com.course.repository;

public interface TeacherClassTopicStatsProjection {
    Integer getCourseId();
    Integer getClassId();
    String getClassName();
    Integer getTeacherId();
    String getTeacherName();
    String getTopic();

    Long getStudentsTotal();
    Long getStudentsWithActivity();
    Double getAvgPercent();
    Long getTestsAttempted();
}
