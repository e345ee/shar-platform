package com.course.dto.achievement;

import lombok.Data;

import java.util.List;


@Data
public class MyAchievementsPageResponse {

    private Integer totalAvailable;
    private Integer totalEarned;

    
    private List<com.course.dto.achievement.StudentAchievementResponse> earned;

    
    private List<com.course.dto.achievement.AchievementResponse> recommendations;

    

    public Integer getTotalAvailable() {
        return this.totalAvailable;
    }

    public void setTotalAvailable(Integer totalAvailable) {
        this.totalAvailable = totalAvailable;
    }

    public Integer getTotalEarned() {
        return this.totalEarned;
    }

    public void setTotalEarned(Integer totalEarned) {
        this.totalEarned = totalEarned;
    }

    public List<com.course.dto.achievement.StudentAchievementResponse> getEarned() {
        return this.earned;
    }

    public void setEarned(List<com.course.dto.achievement.StudentAchievementResponse> earned) {
        this.earned = earned;
    }

    public List<com.course.dto.achievement.AchievementResponse> getRecommendations() {
        return this.recommendations;
    }

    public void setRecommendations(List<com.course.dto.achievement.AchievementResponse> recommendations) {
        this.recommendations = recommendations;
    }

}
