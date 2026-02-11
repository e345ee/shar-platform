package com.course.dto.attempt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

import com.course.dto.attempt.AttemptGradeAnswerRequest;

@Data
public class AttemptGradeRequest {
    @NotEmpty(message = "grades must not be empty")
    private List<@Valid AttemptGradeAnswerRequest> grades;
}
