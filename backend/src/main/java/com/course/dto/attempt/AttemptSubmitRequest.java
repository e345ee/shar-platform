package com.course.dto.attempt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

import com.course.dto.attempt.AttemptSubmitAnswerRequest;

@Data
public class AttemptSubmitRequest {
    @NotEmpty(message = "answers must not be empty")
    private List<@Valid AttemptSubmitAnswerRequest> answers;
}
