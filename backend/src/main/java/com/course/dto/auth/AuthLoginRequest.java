package com.course.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthLoginRequest {

    
    @NotBlank(message = "username is required")
    @Size(min = 1, max = 127, message = "username must be between 1 and 127 characters")
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 1, max = 127, message = "password must be between 1 and 127 characters")
    private String password;

    

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
