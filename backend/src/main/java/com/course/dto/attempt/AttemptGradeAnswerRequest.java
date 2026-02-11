package com.course.dto.attempt;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AttemptGradeAnswerRequest {
    @NotNull
    private Integer questionId;

    
    @NotNull
    @Min(value = 0, message = "pointsAwarded must be >= 0")
    @Max(value = 1000000, message = "pointsAwarded is too large")
    private Integer pointsAwarded;

    
    @Size(max = 2048, message = "feedback must be <= 2048 characters")
    private String feedback;

    

    public Integer getQuestionId() {
        return this.questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public Integer getPointsAwarded() {
        return this.pointsAwarded;
    }

    public void setPointsAwarded(Integer pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }

    public String getFeedback() {
        return this.feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

}
