package com.course.repository;

import java.time.LocalDateTime;

public interface LatestAttemptProjection {
    Integer getTestId();
    Integer getAttemptId();
    String getStatus();
    Integer getScore();
    Integer getMaxScore();
    Integer getWeightedScore();
    Integer getWeightedMaxScore();
    LocalDateTime getSubmittedAt();
}
