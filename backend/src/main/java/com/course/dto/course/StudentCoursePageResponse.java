package com.course.dto.course;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class StudentCoursePageResponse {
    private com.course.dto.course.CourseResponse course;

    
    private boolean courseClosed;
    private List<com.course.dto.lesson.LessonWithActivitiesResponse> lessons = new ArrayList<>();
    
    private List<com.course.dto.activity.ActivityWithAttemptResponse> weeklyThisWeek = new ArrayList<>();

    
    private List<com.course.dto.activity.ActivityWithAttemptResponse> remedialThisWeek = new ArrayList<>();

    

    public com.course.dto.course.CourseResponse getCourse() {
        return this.course;
    }

    public void setCourse(com.course.dto.course.CourseResponse course) {
        this.course = course;
    }

    public boolean isCourseClosed() {
        return this.courseClosed;
    }

    public void setCourseClosed(boolean courseClosed) {
        this.courseClosed = courseClosed;
    }

}
