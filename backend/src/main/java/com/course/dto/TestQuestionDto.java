package com.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TestQuestionDto {
    private Integer id;
    private Integer testId;
    private Integer orderIndex;
    private String questionText;
    private String questionType;
    private Integer points;
    private String option1;
    private String option2;
    private String option3;
    private String option4;

    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer correctOption;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String correctTextAnswer;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
