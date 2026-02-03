package com.course.repository;

import java.time.LocalDateTime;

/**
 * Projection for pending manual-grading attempts (native query result).
 */
public interface PendingAttemptProjection {
    Integer getAttemptId();
    Integer getTestId();
    Integer getLessonId();
    Integer getCourseId();
    Integer getClassId();
    String getClassName();
    Integer getStudentId();
    String getStudentName();
    Integer getUngradedOpenCount();
    LocalDateTime getSubmittedAt();
}
