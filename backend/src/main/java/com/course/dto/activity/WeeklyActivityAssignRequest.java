package com.course.dto.activity;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;


@Data
public class WeeklyActivityAssignRequest {
    
    @NotNull(message = "weekStart is required")
    private LocalDate weekStart;

    @AssertTrue(message = "weekStart must be a Monday (ISO day 1)")
    private boolean isMonday() {
        return weekStart == null || weekStart.getDayOfWeek().getValue() == 1;
    }

    

    public LocalDate getWeekStart() {
        return this.weekStart;
    }

    public void setWeekStart(LocalDate weekStart) {
        this.weekStart = weekStart;
    }

}
