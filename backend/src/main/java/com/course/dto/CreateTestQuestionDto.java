package com.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTestQuestionDto {

    /** Optional; if absent, will be auto-assigned as last+1. */
    @Min(1)
    private Integer orderIndex;

    @NotBlank
    @Size(min = 1, max = 2048)
    private String questionText;

    /**
     * Question type: SINGLE_CHOICE (legacy, default) or TEXT.
     */
    private String questionType;

    /**
     * How many points the question is worth (default 1).
     */
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

    /** 1..4 (index of correct option) for SINGLE_CHOICE questions. */
    private Integer correctOption;

    /** Correct answer for TEXT questions. */
    @Size(max = 512)
    private String correctTextAnswer;
}
