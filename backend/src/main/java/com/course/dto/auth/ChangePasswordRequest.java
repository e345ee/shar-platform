package com.course.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Current password cannot be blank")
    @Size(max = 127, message = "Current password must be up to 127 characters")
    private String currentPassword;

    @NotBlank(message = "New password cannot be blank")
    @Size(min = 6, max = 127, message = "New password must be between 6 and 127 characters")
    private String newPassword;

    

    public String getCurrentPassword() {
        return this.currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return this.newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

}
