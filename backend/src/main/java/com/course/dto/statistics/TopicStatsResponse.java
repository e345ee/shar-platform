package com.course.dto.statistics;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopicStatsResponse {
    private Object courseId;
    private String courseName;

    private Object classId;
    private String className;

    private Object teacherId;
    private String teacherName;

    private String topic;

    private Long studentsTotal;
    private Long studentsWithActivity;
    private Double avgPercent;
    private Long testsAttempted;

    

    public Object getCourseId() {
        return this.courseId;
    }

    public void setCourseId(Object courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return this.courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Object getClassId() {
        return this.classId;
    }

    public void setClassId(Object classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return this.className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Object getTeacherId() {
        return this.teacherId;
    }

    public void setTeacherId(Object teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return this.teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Long getStudentsTotal() {
        return this.studentsTotal;
    }

    public void setStudentsTotal(Long studentsTotal) {
        this.studentsTotal = studentsTotal;
    }

    public Long getStudentsWithActivity() {
        return this.studentsWithActivity;
    }

    public void setStudentsWithActivity(Long studentsWithActivity) {
        this.studentsWithActivity = studentsWithActivity;
    }

    public Double getAvgPercent() {
        return this.avgPercent;
    }

    public void setAvgPercent(Double avgPercent) {
        this.avgPercent = avgPercent;
    }

    public Long getTestsAttempted() {
        return this.testsAttempted;
    }

    public void setTestsAttempted(Long testsAttempted) {
        this.testsAttempted = testsAttempted;
    }

}
