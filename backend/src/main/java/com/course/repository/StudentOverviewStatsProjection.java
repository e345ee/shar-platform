package com.course.repository;

public interface StudentOverviewStatsProjection {
    Long getAttemptsTotal();
    Long getAttemptsInProgress();
    Long getAttemptsFinished();
    Long getAttemptsGraded();

    Long getTestsFinished();
    Long getTestsGraded();

    Long getCoursesStarted();
    Long getCoursesCompleted();
}
