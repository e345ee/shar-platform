package com.course.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TestAttemptAnswerDto {
    private Integer id;
    private Integer attemptId;
    private Integer questionId;
    private Integer questionOrderIndex;
    private Integer selectedOption;

    /**
     * null while attempt is IN_PROGRESS
     */
    private Boolean isCorrect;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
