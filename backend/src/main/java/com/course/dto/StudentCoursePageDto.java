package com.course.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated payload for the student's course page to avoid multiple round-trips.
 */
@Data
public class StudentCoursePageDto {
    private CourseDto course;

    /**
     * True if teacher/methodist marked the course as closed (completed) for this student.
     */
    private boolean courseClosed;
    private List<LessonWithActivitiesDto> lessons = new ArrayList<>();
    /**
     * Weekly activities assigned to the current week.
     */
    private List<ActivityWithAttemptDto> weeklyThisWeek = new ArrayList<>();

    /**
     * Remedial activities ("задания для отстающих") assigned to the student for the current week.
     */
    private List<ActivityWithAttemptDto> remedialThisWeek = new ArrayList<>();
}
