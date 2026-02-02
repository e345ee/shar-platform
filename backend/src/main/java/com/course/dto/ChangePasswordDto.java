package com.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDto {

    @NotBlank(message = "Current password cannot be blank")
    @Size(max = 127, message = "Current password must be up to 127 characters")
    private String currentPassword;

    @NotBlank(message = "New password cannot be blank")
    @Size(max = 127, message = "New password must be up to 127 characters")
    private String newPassword;
}
