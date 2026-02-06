package com.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class TestQuestionUpsertDto {

    
    @Min(1)
    private Integer orderIndex;

    @NotBlank
    @Size(min = 1, max = 2048)
    private String questionText;

    
    private String questionType;

    
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

    
    private Integer correctOption;

    
    @Size(max = 512)
    private String correctTextAnswer;
}
