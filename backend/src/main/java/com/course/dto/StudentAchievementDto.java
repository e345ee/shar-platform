package com.course.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentAchievementDto {
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
}
