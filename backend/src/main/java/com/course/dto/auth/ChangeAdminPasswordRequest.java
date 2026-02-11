package com.course.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangeAdminPasswordRequest {

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 1, max = 127, message = "Password must be between 1 and 127 characters")
    private String newPassword;

    

    public String getNewPassword() {
        return this.newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

}
