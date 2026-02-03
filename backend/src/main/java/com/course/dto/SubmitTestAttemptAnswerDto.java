package com.course.dto;

import lombok.Data;

@Data
public class SubmitTestAttemptAnswerDto {
    private Integer questionId;
    /**
     * selected option (1..4)
     */
    private Integer selectedOption;
}
