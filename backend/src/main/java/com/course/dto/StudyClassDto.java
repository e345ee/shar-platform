package com.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudyClassDto {
    private Integer id;

    @NotBlank
    @Size(min = 1, max = 127)
    private String name;

    @NotNull
    private Integer courseId;

    private Integer teacherId;
    private String teacherName;

    private Integer createdById;
    private String createdByName;

    // 8-char code for student join requests
    private String joinCode;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
