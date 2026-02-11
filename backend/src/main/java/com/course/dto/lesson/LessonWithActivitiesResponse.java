package com.course.dto.lesson;

import lombok.Data;

import java.util.List;

@Data
public class LessonWithActivitiesResponse {
    private com.course.dto.lesson.LessonResponse lesson;
    private List<com.course.dto.activity.ActivityWithAttemptResponse> activities;

    

    public com.course.dto.lesson.LessonResponse getLesson() {
        return this.lesson;
    }

    public void setLesson(com.course.dto.lesson.LessonResponse lesson) {
        this.lesson = lesson;
    }

    public List<com.course.dto.activity.ActivityWithAttemptResponse> getActivities() {
        return this.activities;
    }

    public void setActivities(List<com.course.dto.activity.ActivityWithAttemptResponse> activities) {
        this.activities = activities;
    }

}
