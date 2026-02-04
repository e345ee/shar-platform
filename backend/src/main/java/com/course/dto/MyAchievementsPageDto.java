package com.course.dto;

import lombok.Data;

import java.util.List;

/**
 * Response for "My achievements" page.
 * Reuses existing AchievementDto and StudentAchievementDto to avoid DTO duplication.
 */
@Data
public class MyAchievementsPageDto {

    private Integer totalAvailable;
    private Integer totalEarned;

    /** Earned achievements (with conditions info). */
    private List<StudentAchievementDto> earned;

    /** Not yet earned achievements (can be used as recommendations). */
    private List<AchievementDto> recommendations;
}
