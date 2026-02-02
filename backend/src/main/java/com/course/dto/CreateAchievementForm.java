package com.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CreateAchievementForm {

    @NotBlank
    @Size(min = 1, max = 127)
    private String title;

    @NotBlank
    @Size(min = 1, max = 1024)
    private String jokeDescription;

    @NotBlank
    @Size(min = 1, max = 2048)
    private String description;

    // validated in service (content-type, size, etc)
    private MultipartFile photo;
}
