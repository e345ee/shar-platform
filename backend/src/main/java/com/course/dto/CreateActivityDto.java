package com.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Generic activity creation request. Used for:
 * - HOMEWORK_TEST (lessonId required)
 * - CONTROL_WORK (lessonId optional)
 * - WEEKLY_STAR (lessonId must be null; assigned to a week separately)
 * - REMEDIAL_TASK (lessonId must be null; can be assigned to a week; shown only to assigned students)
 */
@Data
public class CreateActivityDto {

    @NotNull
    private String activityType; // HOMEWORK_TEST | CONTROL_WORK | WEEKLY_STAR | REMEDIAL_TASK

    /** Required for lesson-attached activities */
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

    /** Optional; if null will be set by defaults for the type */
    private Integer weightMultiplier;
}
