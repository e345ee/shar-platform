package com.course.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class StudentCourseProgressDto {
    /**
     * Serialized as string because e2e tests use jq with --arg (string) and
     * compare with strict equality.
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Integer courseId;
    private String courseName;

    /** Required (lesson-bound) activities in the course (HOMEWORK_TEST + CONTROL_WORK, READY). */
    private Long requiredTests;
    /** How many required activities are finished (SUBMITTED/GRADED). */
    private Long completedTests;

    /** 0..100 */
    private Double percent;
    private Boolean completed;
}
