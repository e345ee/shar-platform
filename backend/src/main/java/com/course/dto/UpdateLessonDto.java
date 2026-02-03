package com.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateLessonDto {

    /** Optional; if provided, reorders the lesson inside its course. */
    @Min(1)
    private Integer orderIndex;

    @NotBlank
    @Size(min = 1, max = 127)
    private String title;

    @Size(max = 2048)
    private String description;
}
