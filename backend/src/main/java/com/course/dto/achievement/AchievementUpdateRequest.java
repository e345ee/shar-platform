package com.course.dto.achievement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AchievementUpdateRequest {

    @NotBlank(message = "title is required")
    @Size(min = 1, max = 127, message = "title must be between 1 and 127 characters")
    private String title;

    @NotBlank(message = "jokeDescription is required")
    @Size(min = 1, max = 1024, message = "jokeDescription must be between 1 and 1024 characters")
    private String jokeDescription;

    @NotBlank(message = "description is required")
    @Size(min = 1, max = 2048, message = "description must be between 1 and 2048 characters")
    private String description;

    

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getJokeDescription() {
        return this.jokeDescription;
    }

    public void setJokeDescription(String jokeDescription) {
        this.jokeDescription = jokeDescription;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
