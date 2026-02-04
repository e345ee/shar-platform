package com.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request body for creating or updating a test.
 */
@Data
public class TestUpsertDto {

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
}
