package com.course.dto.attempt;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttemptStatusResponse {
    private Integer testId;
    private Integer attemptId;
    private String status;
    private Integer score;
    private Integer maxScore;
    private Integer weightedScore;
    private Integer weightedMaxScore;
    private LocalDateTime submittedAt;

    

    public Integer getTestId() {
        return this.testId;
    }

    public void setTestId(Integer testId) {
        this.testId = testId;
    }

    public Integer getAttemptId() {
        return this.attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getScore() {
        return this.score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getMaxScore() {
        return this.maxScore;
    }

    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
    }

    public Integer getWeightedScore() {
        return this.weightedScore;
    }

    public void setWeightedScore(Integer weightedScore) {
        this.weightedScore = weightedScore;
    }

    public Integer getWeightedMaxScore() {
        return this.weightedMaxScore;
    }

    public void setWeightedMaxScore(Integer weightedMaxScore) {
        this.weightedMaxScore = weightedMaxScore;
    }

    public LocalDateTime getSubmittedAt() {
        return this.submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

}
