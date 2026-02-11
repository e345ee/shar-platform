package com.course.dto.activity;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class ActivityQuestionUpsertRequest {

    
    @NotNull(message = "orderIndex is required")
    @Min(value = 1, message = "orderIndex must be >= 1")
    private Integer orderIndex;

    @NotBlank(message = "questionText is required")
    @Size(min = 1, max = 2048, message = "questionText must be between 1 and 2048 characters")
    private String questionText;

    
    @NotBlank(message = "questionType is required")
    @Pattern(regexp = "SINGLE_CHOICE|TEXT|OPEN", message = "questionType must be one of: SINGLE_CHOICE, TEXT, OPEN")
    private String questionType;

    
    @NotNull(message = "points is required")
    @Min(value = 1, message = "points must be >= 1")
    private Integer points;

    @Size(max = 512)
    private String option1;

    @Size(max = 512)
    private String option2;

    @Size(max = 512)
    private String option3;

    @Size(max = 512)
    private String option4;

    
    @Min(value = 1, message = "correctOption must be between 1 and 4")
    @Max(value = 4, message = "correctOption must be between 1 and 4")
    private Integer correctOption;

    
    @Size(max = 512, message = "correctTextAnswer must be at most 512 characters")
    private String correctTextAnswer;

    
    @AssertTrue(message = "For SINGLE_CHOICE: options 1-4 and correctOption are required; correctTextAnswer must be null")
    private boolean isSingleChoiceValid() {
        if (!"SINGLE_CHOICE".equals(questionType)) {
            return true;
        }
        return notBlank(option1) && notBlank(option2) && notBlank(option3) && notBlank(option4)
                && correctOption != null
                && correctTextAnswer == null;
    }

    @AssertTrue(message = "For TEXT: correctTextAnswer is required; options and correctOption must be null")
    private boolean isTextValid() {
        if (!"TEXT".equals(questionType)) {
            return true;
        }
        return notBlank(correctTextAnswer)
                && option1 == null && option2 == null && option3 == null && option4 == null
                && correctOption == null;
    }

    @AssertTrue(message = "For OPEN: options and correctOption must be null")
    private boolean isOpenValid() {
        if (!"OPEN".equals(questionType)) {
            return true;
        }
        return option1 == null && option2 == null && option3 == null && option4 == null
                && correctOption == null;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    

    public Integer getOrderIndex() {
        return this.orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getQuestionText() {
        return this.questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionType() {
        return this.questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public Integer getPoints() {
        return this.points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getOption1() {
        return this.option1;
    }

    public void setOption1(String option1) {
        this.option1 = option1;
    }

    public String getOption2() {
        return this.option2;
    }

    public void setOption2(String option2) {
        this.option2 = option2;
    }

    public String getOption3() {
        return this.option3;
    }

    public void setOption3(String option3) {
        this.option3 = option3;
    }

    public String getOption4() {
        return this.option4;
    }

    public void setOption4(String option4) {
        this.option4 = option4;
    }

    public Integer getCorrectOption() {
        return this.correctOption;
    }

    public void setCorrectOption(Integer correctOption) {
        this.correctOption = correctOption;
    }

    public String getCorrectTextAnswer() {
        return this.correctTextAnswer;
    }

    public void setCorrectTextAnswer(String correctTextAnswer) {
        this.correctTextAnswer = correctTextAnswer;
    }

}
