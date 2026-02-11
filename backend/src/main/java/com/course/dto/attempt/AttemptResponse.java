package com.course.dto.attempt;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AttemptResponse {
    private Integer id;

    private Integer testId;
    private Integer lessonId;
    private Integer courseId;

    private Integer studentId;
    private String studentName;

    private Integer attemptNumber;
    private String status;

    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    private Integer score;
    private Integer maxScore;
    private Double percent;

    private Integer weightedScore;
    private Integer weightedMaxScore;
    private Double weightedPercent;

    private List<com.course.dto.attempt.AttemptAnswerResponse> answers;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTestId() {
        return this.testId;
    }

    public void setTestId(Integer testId) {
        this.testId = testId;
    }

    public Integer getLessonId() {
        return this.lessonId;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }

    public Integer getCourseId() {
        return this.courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getStudentId() {
        return this.studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return this.studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Integer getAttemptNumber() {
        return this.attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return this.startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return this.submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
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

    public Double getPercent() {
        return this.percent;
    }

    public void setPercent(Double percent) {
        this.percent = percent;
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

    public Double getWeightedPercent() {
        return this.weightedPercent;
    }

    public void setWeightedPercent(Double weightedPercent) {
        this.weightedPercent = weightedPercent;
    }

    public List<com.course.dto.attempt.AttemptAnswerResponse> getAnswers() {
        return this.answers;
    }

    public void setAnswers(List<com.course.dto.attempt.AttemptAnswerResponse> answers) {
        this.answers = answers;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
