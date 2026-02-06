package com.course.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class StudentCoursePageDto {
    private CourseDto course;

    
    private boolean courseClosed;
    private List<LessonWithActivitiesDto> lessons = new ArrayList<>();
    
    private List<ActivityWithAttemptDto> weeklyThisWeek = new ArrayList<>();

    
    private List<ActivityWithAttemptDto> remedialThisWeek = new ArrayList<>();
}
