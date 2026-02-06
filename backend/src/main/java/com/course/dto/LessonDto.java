package com.course.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LessonDto {
    private Integer id;
    private Integer courseId;

    
    private Integer orderIndex;

    private String title;
    private String description;

    
    private String presentationUrl;

    private Integer createdById;
    private String createdByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
