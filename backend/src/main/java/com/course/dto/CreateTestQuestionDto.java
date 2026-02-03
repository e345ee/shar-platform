package com.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank
    @Size(min = 1, max = 512)
    private String option1;

    @NotBlank
    @Size(min = 1, max = 512)
    private String option2;

    @NotBlank
    @Size(min = 1, max = 512)
    private String option3;

    @NotBlank
    @Size(min = 1, max = 512)
    private String option4;

    /** 1..4 (index of correct option). */
    @NotNull
    @Min(1)
    @Max(4)
    private Integer correctOption;
}
