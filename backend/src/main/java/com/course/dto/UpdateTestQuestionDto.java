package com.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTestQuestionDto {

    @Min(1)
    private Integer orderIndex;

    @NotBlank
    @Size(min = 1, max = 2048)
    private String questionText;

    /** Question type: SINGLE_CHOICE or TEXT. If null, keeps current type. */
    private String questionType;

    /** How many points the question is worth. If null, keeps current value. */
    @Min(1)
    private Integer points;

    @Size(max = 512)
    private String option1;

    @Size(max = 512)
    private String option2;

    @Size(max = 512)
    private String option3;

    @Size(max = 512)
    private String option4;

    /** 1..4 for SINGLE_CHOICE questions. */
    private Integer correctOption;

    /** Correct answer for TEXT questions. */
    @Size(max = 512)
    private String correctTextAnswer;
}
