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

    /** Total students in the class. */
    private Long studentsTotal;
    /** Students who have at least one finished attempt in this topic. */
    private Long studentsWithActivity;

    /** Avg percent across students (avg of student's avg-best-per-test). */
    private Double avgPercent;

    /** Sum of distinct tests attempted across students (best attempt per test). */
    private Long testsAttempted;
}
