package com.course.dto.activity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class ActivityUpsertRequest {

    @NotBlank(message = "title is required")
    @Size(min = 1, max = 127, message = "title must be between 1 and 127 characters")
    private String title;

    @Pattern(regexp = "^(?!\\s*$).+", message = "description must not be blank")
    @Size(max = 2048, message = "description must be at most 2048 characters")
    private String description;

    @NotBlank(message = "topic is required")
    @Size(min = 1, max = 127, message = "topic must be between 1 and 127 characters")
    private String topic;

    @NotNull(message = "deadline is required")
    private LocalDateTime deadline;

    
    @Min(value = 1, message = "timeLimitSeconds must be between 1 and 86400")
    @Max(value = 86400, message = "timeLimitSeconds must be between 1 and 86400")
    private Integer timeLimitSeconds;

    

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public LocalDateTime getDeadline() {
        return this.deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public Integer getTimeLimitSeconds() {
        return this.timeLimitSeconds;
    }

    public void setTimeLimitSeconds(Integer timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
    }

}
