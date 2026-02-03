package com.course.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitTestAttemptAnswerDto {
    @NotNull
    private Integer questionId;
    /**
     * selected option (1..4)
     */
    private Integer selectedOption;

    /**
     * Text answer for TEXT / OPEN questions.
     */
    @Size(max = 4096, message = "textAnswer length must be <= 4096")
    private String textAnswer;
}
