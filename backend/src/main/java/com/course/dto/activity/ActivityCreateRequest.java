package com.course.dto.activity;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class ActivityCreateRequest {

    @NotBlank(message = "activityType is required")
    @Pattern(regexp = "HOMEWORK_TEST|CONTROL_WORK|WEEKLY_STAR|REMEDIAL_TASK", message = "activityType must be one of: HOMEWORK_TEST, CONTROL_WORK, WEEKLY_STAR, REMEDIAL_TASK")
    private String activityType;

    
    private Integer lessonId;

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

    
    @Min(value = 1, message = "weightMultiplier must be between 1 and 100")
    @Max(value = 100, message = "weightMultiplier must be between 1 and 100")
    private Integer weightMultiplier;

    
    @Min(value = 1, message = "timeLimitSeconds must be between 1 and 86400")
    @Max(value = 86400, message = "timeLimitSeconds must be between 1 and 86400")
    private Integer timeLimitSeconds;

    
    @AssertTrue(message = "For WEEKLY_STAR and REMEDIAL_TASK lessonId must be null")
    private boolean isLessonIdValidForType() {
        if (activityType == null) {
            return true;
        }
        boolean weeklyOrRemedial = "WEEKLY_STAR".equals(activityType) || "REMEDIAL_TASK".equals(activityType);
        return !weeklyOrRemedial || lessonId == null;
    }

    

    public String getActivityType() {
        return this.activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public Integer getLessonId() {
        return this.lessonId;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }

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

    public Integer getWeightMultiplier() {
        return this.weightMultiplier;
    }

    public void setWeightMultiplier(Integer weightMultiplier) {
        this.weightMultiplier = weightMultiplier;
    }

    public Integer getTimeLimitSeconds() {
        return this.timeLimitSeconds;
    }

    public void setTimeLimitSeconds(Integer timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
    }

}
