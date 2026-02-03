package com.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CreateLessonForm {

    /** Optional; if null the lesson is appended to the end. */
    @Min(1)
    private Integer orderIndex;

    @NotBlank
    @Size(min = 1, max = 127)
    private String title;

    @Size(max = 2048)
    private String description;

    // validated in service (content-type, size, etc)
    private MultipartFile presentation;
}
