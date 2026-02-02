package com.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAchievementDto {

    @NotBlank
    @Size(min = 1, max = 127)
    private String title;

    @NotBlank
    @Size(min = 1, max = 1024)
    private String jokeDescription;

    @NotBlank
    @Size(min = 1, max = 2048)
    private String description;
}
