package com.course.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;


@Data
public class AssignWeeklyActivityDto {
    
    @NotNull
    private LocalDate weekStart;
}
