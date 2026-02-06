package com.course.dto;

import lombok.Data;


@Data
public class TeacherStatsDto {
    private Integer teacherId;
    private String teacherName;
    private String teacherEmail;
    private Long classesCount;
    private Long studentsCount;
    private Long submittedAttemptsCount;
    private Long gradedAttemptsCount;
    
    private Double avgGradePercent;
}
