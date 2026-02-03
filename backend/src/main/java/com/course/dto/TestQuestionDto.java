package com.course.dto;

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
    private Integer correctOption;
    private String correctTextAnswer;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
