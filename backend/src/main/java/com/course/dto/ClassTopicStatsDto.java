package com.course.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class ClassTopicStatsDto {
    @JsonSerialize(using = ToStringSerializer.class)
    private Integer classId;
    private String className;
    @JsonSerialize(using = ToStringSerializer.class)
    private Integer courseId;
    private String topic;

    
    private Long studentsTotal;
    
    private Long studentsWithActivity;

    
    private Double avgPercent;

    
    private Long testsAttempted;
}
