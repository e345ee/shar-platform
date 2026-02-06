package com.course.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthLoginRequestDto {

    
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
