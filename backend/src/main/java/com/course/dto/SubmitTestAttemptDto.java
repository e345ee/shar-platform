package com.course.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SubmitTestAttemptDto {
    @NotEmpty(message = "answers must not be empty")
    private List<@Valid SubmitTestAttemptAnswerDto> answers;
}
