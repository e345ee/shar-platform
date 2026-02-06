package com.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GradeTestAttemptAnswerDto {
    @NotNull
    private Integer questionId;

    
    @NotNull
    @Min(value = 0, message = "pointsAwarded must be >= 0")
    @Max(value = 1000000, message = "pointsAwarded is too large")
    private Integer pointsAwarded;

    
    @Size(max = 2048, message = "feedback must be <= 2048 characters")
    private String feedback;
}
