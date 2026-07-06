package com.course.repository;

public interface ClassTopicStatsProjection {
    Integer getClassId();
    String getClassName();
    Integer getCourseId();
    String getTopic();

    Long getStudentsTotal();
    Long getStudentsWithActivity();
    Double getAvgPercent();
    Long getTestsAttempted();
}
