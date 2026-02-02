package com.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseDto {
    private Integer id;

    @NotBlank
    @Size(min = 1, max = 127)
    private String name;

    private String description;

    private Integer createdById;
    private String createdByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
