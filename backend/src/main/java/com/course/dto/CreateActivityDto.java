package com.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class CreateActivityDto {

    @NotNull
    private String activityType; 

    
    private Integer lessonId;

    @NotBlank
    @Size(min = 1, max = 127)
    private String title;

    @Size(max = 2048)
    private String description;

    @NotBlank
    @Size(min = 1, max = 127)
    private String topic;

    @NotNull
    private LocalDateTime deadline;

    
    private Integer weightMultiplier;

    
    private Integer timeLimitSeconds;
}
