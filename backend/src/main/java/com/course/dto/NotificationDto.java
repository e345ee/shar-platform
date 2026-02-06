package com.course.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDto {
    private Integer id;
    private String type;
    private String title;
    private String message;
    private boolean read;

    private Integer courseId;
    private Integer classId;
    private Integer testId;
    private Integer attemptId;
    private Integer achievementId;

    private LocalDateTime createdAt;
}
