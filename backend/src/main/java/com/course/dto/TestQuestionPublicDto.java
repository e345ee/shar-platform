package com.course.dto;

import lombok.Data;

@Data
public class TestQuestionPublicDto {
    private Integer id;
    private Integer testId;
    private Integer orderIndex;
    private String questionText;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
}
