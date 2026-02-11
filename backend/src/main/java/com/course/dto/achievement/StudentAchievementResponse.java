package com.course.dto.achievement;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentAchievementResponse {
    private Integer id;

    private Integer studentId;
    private String studentName;

    private Integer achievementId;
    private String achievementTitle;
    private String achievementPhotoUrl;

    
    private Integer achievementCourseId;
    private String achievementJokeDescription;
    private String achievementDescription;

    private Integer awardedById;
    private String awardedByName;

    private LocalDateTime awardedAt;

    

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStudentId() {
        return this.studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return this.studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Integer getAchievementId() {
        return this.achievementId;
    }

    public void setAchievementId(Integer achievementId) {
        this.achievementId = achievementId;
    }

    public String getAchievementTitle() {
        return this.achievementTitle;
    }

    public void setAchievementTitle(String achievementTitle) {
        this.achievementTitle = achievementTitle;
    }

    public String getAchievementPhotoUrl() {
        return this.achievementPhotoUrl;
    }

    public void setAchievementPhotoUrl(String achievementPhotoUrl) {
        this.achievementPhotoUrl = achievementPhotoUrl;
    }

    public Integer getAchievementCourseId() {
        return this.achievementCourseId;
    }

    public void setAchievementCourseId(Integer achievementCourseId) {
        this.achievementCourseId = achievementCourseId;
    }

    public String getAchievementJokeDescription() {
        return this.achievementJokeDescription;
    }

    public void setAchievementJokeDescription(String achievementJokeDescription) {
        this.achievementJokeDescription = achievementJokeDescription;
    }

    public String getAchievementDescription() {
        return this.achievementDescription;
    }

    public void setAchievementDescription(String achievementDescription) {
        this.achievementDescription = achievementDescription;
    }

    public Integer getAwardedById() {
        return this.awardedById;
    }

    public void setAwardedById(Integer awardedById) {
        this.awardedById = awardedById;
    }

    public String getAwardedByName() {
        return this.awardedByName;
    }

    public void setAwardedByName(String awardedByName) {
        this.awardedByName = awardedByName;
    }

    public LocalDateTime getAwardedAt() {
        return this.awardedAt;
    }

    public void setAwardedAt(LocalDateTime awardedAt) {
        this.awardedAt = awardedAt;
    }

}
