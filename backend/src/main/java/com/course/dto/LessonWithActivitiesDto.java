package com.course.dto;

import lombok.Data;

import java.util.List;

@Data
public class LessonWithActivitiesDto {
    private LessonDto lesson;
    private List<ActivityWithAttemptDto> activities;
}
