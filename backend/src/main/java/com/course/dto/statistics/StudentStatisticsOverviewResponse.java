package com.course.dto.statistics;

import lombok.Data;

import java.util.List;

@Data
public class StudentStatisticsOverviewResponse {
    private Long attemptsTotal;
    private Long attemptsInProgress;
    private Long attemptsFinished;
    private Long attemptsGraded;

    private Long testsFinished;
    private Long testsGraded;

    private Long coursesStarted;
    private Long coursesCompleted;

    private List<com.course.dto.course.StudentCourseProgressResponse> courses;

    

    public Long getAttemptsTotal() {
        return this.attemptsTotal;
    }

    public void setAttemptsTotal(Long attemptsTotal) {
        this.attemptsTotal = attemptsTotal;
    }

    public Long getAttemptsInProgress() {
        return this.attemptsInProgress;
    }

    public void setAttemptsInProgress(Long attemptsInProgress) {
        this.attemptsInProgress = attemptsInProgress;
    }

    public Long getAttemptsFinished() {
        return this.attemptsFinished;
    }

    public void setAttemptsFinished(Long attemptsFinished) {
        this.attemptsFinished = attemptsFinished;
    }

    public Long getAttemptsGraded() {
        return this.attemptsGraded;
    }

    public void setAttemptsGraded(Long attemptsGraded) {
        this.attemptsGraded = attemptsGraded;
    }

    public Long getTestsFinished() {
        return this.testsFinished;
    }

    public void setTestsFinished(Long testsFinished) {
        this.testsFinished = testsFinished;
    }

    public Long getTestsGraded() {
        return this.testsGraded;
    }

    public void setTestsGraded(Long testsGraded) {
        this.testsGraded = testsGraded;
    }

    public Long getCoursesStarted() {
        return this.coursesStarted;
    }

    public void setCoursesStarted(Long coursesStarted) {
        this.coursesStarted = coursesStarted;
    }

    public Long getCoursesCompleted() {
        return this.coursesCompleted;
    }

    public void setCoursesCompleted(Long coursesCompleted) {
        this.coursesCompleted = coursesCompleted;
    }

    public List<com.course.dto.course.StudentCourseProgressResponse> getCourses() {
        return this.courses;
    }

    public void setCourses(List<com.course.dto.course.StudentCourseProgressResponse> courses) {
        this.courses = courses;
    }

}
