package com.course.dto.activity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivityResponse {
    private Integer id;
    private Integer lessonId;
    private Integer courseId;

    private String activityType;
    private Integer weightMultiplier;
    private java.time.LocalDate assignedWeekStart;
    private Integer timeLimitSeconds;

    private String title;
    private String description;
    private String topic;
    private LocalDateTime deadline;

    
    private String status;
    private LocalDateTime publishedAt;

    private Integer createdById;
    private String createdByName;

    private Integer questionCount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<com.course.dto.activity.ActivityQuestionResponse> questions;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getActivityType() {
        return this.activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public Integer getWeightMultiplier() {
        return this.weightMultiplier;
    }

    public void setWeightMultiplier(Integer weightMultiplier) {
        this.weightMultiplier = weightMultiplier;
    }

    public java.time.LocalDate getAssignedWeekStart() {
        return this.assignedWeekStart;
    }

    public void setAssignedWeekStart(java.time.LocalDate assignedWeekStart) {
        this.assignedWeekStart = assignedWeekStart;
    }

    public Integer getTimeLimitSeconds() {
        return this.timeLimitSeconds;
    }

    public void setTimeLimitSeconds(Integer timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public LocalDateTime getDeadline() {
        return this.deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPublishedAt() {
        return this.publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Integer getCreatedById() {
        return this.createdById;
    }

    public void setCreatedById(Integer createdById) {
        this.createdById = createdById;
    }

    public String getCreatedByName() {
        return this.createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public Integer getQuestionCount() {
        return this.questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public List<com.course.dto.activity.ActivityQuestionResponse> getQuestions() {
        return this.questions;
    }

    public void setQuestions(List<com.course.dto.activity.ActivityQuestionResponse> questions) {
        this.questions = questions;
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
