package com.course.dto;

import lombok.Data;

import java.util.List;

@Data
public class StudentStatisticsOverviewDto {
    private Long attemptsTotal;
    private Long attemptsInProgress;
    private Long attemptsFinished;
    private Long attemptsGraded;

    private Long testsFinished;
    private Long testsGraded;

    private Long coursesStarted;
    private Long coursesCompleted;

    private List<StudentCourseProgressDto> courses;
}
