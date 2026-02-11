package com.course.dto.attempt;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttemptAnswerResponse {
    private Integer id;
    private Integer attemptId;
    private Integer questionId;
    private Integer questionOrderIndex;
    private Integer selectedOption;
    private String textAnswer;

    
    private String feedback;

    
    private LocalDateTime gradedAt;

    
    private Integer pointsAwarded;

    
    private Boolean isCorrect;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAttemptId() {
        return this.attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer getQuestionId() {
        return this.questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public Integer getQuestionOrderIndex() {
        return this.questionOrderIndex;
    }

    public void setQuestionOrderIndex(Integer questionOrderIndex) {
        this.questionOrderIndex = questionOrderIndex;
    }

    public Integer getSelectedOption() {
        return this.selectedOption;
    }

    public void setSelectedOption(Integer selectedOption) {
        this.selectedOption = selectedOption;
    }

    public String getTextAnswer() {
        return this.textAnswer;
    }

    public void setTextAnswer(String textAnswer) {
        this.textAnswer = textAnswer;
    }

    public String getFeedback() {
        return this.feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public LocalDateTime getGradedAt() {
        return this.gradedAt;
    }

    public void setGradedAt(LocalDateTime gradedAt) {
        this.gradedAt = gradedAt;
    }

    public Integer getPointsAwarded() {
        return this.pointsAwarded;
    }

    public void setPointsAwarded(Integer pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }

    public Boolean getIsCorrect() {
        return this.isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
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
