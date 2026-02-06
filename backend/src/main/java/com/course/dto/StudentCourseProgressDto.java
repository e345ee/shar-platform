package com.course.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class StudentCourseProgressDto {
    
    @JsonSerialize(using = ToStringSerializer.class)
    private Integer courseId;
    private String courseName;

    
    private Long requiredTests;
    
    private Long completedTests;

    
    private Double percent;
    private Boolean completed;
}
