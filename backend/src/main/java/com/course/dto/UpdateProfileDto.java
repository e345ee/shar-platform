package com.course.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileDto {

    @Size(max = 63, message = "Name must be up to 63 characters")
    private String name;

    @Size(max = 5000, message = "Bio must be up to 5000 characters")
    private String bio;

    @Size(max = 5000, message = "Photo must be up to 5000 characters")
    private String photo;

    @Size(max = 127, message = "Password must be up to 127 characters")
    private String password;
}
