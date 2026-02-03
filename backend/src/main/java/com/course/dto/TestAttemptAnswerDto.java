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
    private String textAnswer;

    /** Teacher feedback for OPEN questions (optional). */
    private String feedback;

    /** When teacher graded the OPEN answer (null if not graded yet). */
    private LocalDateTime gradedAt;

    /** Points awarded for the answer (0..question.points). */
    private Integer pointsAwarded;

    /**
     * null while attempt is IN_PROGRESS
     */
    private Boolean isCorrect;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
