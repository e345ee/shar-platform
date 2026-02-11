package com.course.dto.course;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class StudentCourseProgressResponse {
    
    @JsonSerialize(using = ToStringSerializer.class)
    private Integer courseId;
    private String courseName;

    
    private Long requiredTests;
    
    private Long completedTests;

    
    private Double percent;
    private Boolean completed;

    

    public Integer getCourseId() {
        return this.courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return this.courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Long getRequiredTests() {
        return this.requiredTests;
    }

    public void setRequiredTests(Long requiredTests) {
        this.requiredTests = requiredTests;
    }

    public Long getCompletedTests() {
        return this.completedTests;
    }

    public void setCompletedTests(Long completedTests) {
        this.completedTests = completedTests;
    }

    public Double getPercent() {
        return this.percent;
    }

    public void setPercent(Double percent) {
        this.percent = percent;
    }

    public Boolean getCompleted() {
        return this.completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

}
