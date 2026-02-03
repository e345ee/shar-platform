package com.course.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class GradeTestAttemptDto {
    @NotEmpty(message = "grades must not be empty")
    private List<@Valid GradeTestAttemptAnswerDto> grades;
}
