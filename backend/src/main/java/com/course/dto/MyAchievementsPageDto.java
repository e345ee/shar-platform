package com.course.dto;

import lombok.Data;

import java.util.List;


@Data
public class MyAchievementsPageDto {

    private Integer totalAvailable;
    private Integer totalEarned;

    
    private List<StudentAchievementDto> earned;

    
    private List<AchievementDto> recommendations;
}
