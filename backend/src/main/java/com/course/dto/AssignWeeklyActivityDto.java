package com.course.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * Assigns a WEEKLY_STAR activity to a specific week.
 * Week is represented by its Monday date.
 */
@Data
public class AssignWeeklyActivityDto {
    /**
     * Monday date of the week (course calendar). If null, server may use current week.
     */
    @NotNull
    private LocalDate weekStart;
}
