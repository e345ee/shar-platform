package com.course.dto.attempt;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AttemptSubmitAnswerRequest {
    @NotNull(message = "questionId is required")
    @Min(value = 1, message = "questionId must be a positive integer")
    private Integer questionId;
    
    @Min(value = 1, message = "selectedOption must be between 1 and 4")
    @Max(value = 4, message = "selectedOption must be between 1 and 4")
    private Integer selectedOption;

    
    @Pattern(regexp = "^(?!\\s*$).+", message = "textAnswer must not be blank")
    @Size(max = 4096, message = "textAnswer length must be <= 4096")
    private String textAnswer;

    

    public Integer getQuestionId() {
        return this.questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
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

}
