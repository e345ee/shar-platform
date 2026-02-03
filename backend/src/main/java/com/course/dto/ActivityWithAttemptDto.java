package com.course.dto;

import lombok.Data;

@Data
public class ActivityWithAttemptDto {
    private TestSummaryDto activity;
    private AttemptStatusDto latestAttempt;
}
