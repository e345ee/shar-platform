package com.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestAttemptAnswerPublicDto {
    private Integer questionId;
    private Integer orderIndex;
    private Integer selectedOption;
    /** null while IN_PROGRESS; filled after submit */
    private Boolean isCorrect;
}
