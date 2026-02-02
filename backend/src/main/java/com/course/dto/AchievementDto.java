package com.course.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AchievementDto {
    private Integer id;
    private Integer courseId;

    private String title;
    private String jokeDescription;
    private String description;
    private String photoUrl;

    private Integer createdById;
    private String createdByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
