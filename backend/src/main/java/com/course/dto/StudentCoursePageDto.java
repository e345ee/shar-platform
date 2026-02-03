package com.course.dto;

import lombok.Data;

import java.util.List;

/**
 * Aggregated payload for the student's course page to avoid multiple round-trips.
 */
@Data
public class StudentCoursePageDto {
    private CourseDto course;
    private List<LessonWithActivitiesDto> lessons;
    /**
     * Weekly activities assigned to the current week.
     */
    private List<ActivityWithAttemptDto> weeklyThisWeek;
}
