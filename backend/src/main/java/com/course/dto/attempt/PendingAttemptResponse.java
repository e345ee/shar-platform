package com.course.dto.attempt;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PendingAttemptResponse {
    private Integer attemptId;
    private Integer testId;
    private Integer lessonId;
    private Integer courseId;

    private Integer classId;
    private String className;

    private Integer studentId;
    private String studentName;

    private Integer ungradedOpenCount;

    private LocalDateTime submittedAt;

    

    public Integer getAttemptId() {
        return this.attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer getTestId() {
        return this.testId;
    }

    public void setTestId(Integer testId) {
        this.testId = testId;
    }

    public Integer getLessonId() {
        return this.lessonId;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }

    public Integer getCourseId() {
        return this.courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getClassId() {
        return this.classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return this.className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Integer getStudentId() {
        return this.studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return this.studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Integer getUngradedOpenCount() {
        return this.ungradedOpenCount;
    }

    public void setUngradedOpenCount(Integer ungradedOpenCount) {
        this.ungradedOpenCount = ungradedOpenCount;
    }

    public LocalDateTime getSubmittedAt() {
        return this.submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

}
