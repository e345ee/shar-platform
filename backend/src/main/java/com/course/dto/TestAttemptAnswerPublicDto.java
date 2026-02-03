package com.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestAttemptAnswerPublicDto {
    private Integer questionId;
    private Integer orderIndex;
    private Integer selectedOption;
    private String textAnswer;
    private String feedback;
    private LocalDateTime gradedAt;
    private Integer pointsAwarded;
    /** null while IN_PROGRESS; filled after submit */
    private Boolean isCorrect;
}
