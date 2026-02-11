package com.course.dto.statistics;

import lombok.Data;


@Data
public class TeacherStatsResponse {
    private Integer teacherId;
    private String teacherName;
    private String teacherEmail;
    private Long classesCount;
    private Long studentsCount;
    private Long submittedAttemptsCount;
    private Long gradedAttemptsCount;
    
    private Double avgGradePercent;

    

    public Integer getTeacherId() {
        return this.teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return this.teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getTeacherEmail() {
        return this.teacherEmail;
    }

    public void setTeacherEmail(String teacherEmail) {
        this.teacherEmail = teacherEmail;
    }

    public Long getClassesCount() {
        return this.classesCount;
    }

    public void setClassesCount(Long classesCount) {
        this.classesCount = classesCount;
    }

    public Long getStudentsCount() {
        return this.studentsCount;
    }

    public void setStudentsCount(Long studentsCount) {
        this.studentsCount = studentsCount;
    }

    public Long getSubmittedAttemptsCount() {
        return this.submittedAttemptsCount;
    }

    public void setSubmittedAttemptsCount(Long submittedAttemptsCount) {
        this.submittedAttemptsCount = submittedAttemptsCount;
    }

    public Long getGradedAttemptsCount() {
        return this.gradedAttemptsCount;
    }

    public void setGradedAttemptsCount(Long gradedAttemptsCount) {
        this.gradedAttemptsCount = gradedAttemptsCount;
    }

    public Double getAvgGradePercent() {
        return this.avgGradePercent;
    }

    public void setAvgGradePercent(Double avgGradePercent) {
        this.avgGradePercent = avgGradePercent;
    }

}
