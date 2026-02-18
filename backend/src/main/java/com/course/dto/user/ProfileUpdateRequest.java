package com.course.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {

    @Pattern(regexp = "^(?!\\s*$).+", message = "name must not be blank")
    @Size(max = 63, message = "name must be at most 63 characters")
    private String name;

    @Pattern(regexp = "^(?!\\s*$).+", message = "bio must not be blank")
    @Size(max = 2048, message = "bio must be at most 2048 characters")
    private String bio;

    @Pattern(regexp = "^(?!\\s*$).+", message = "email must not be blank")
    @Email(message = "email should be valid")
    @Size(min = 3, max = 127, message = "email must be between 3 and 127 characters")
    private String email;

    @Pattern(regexp = "^(?!\\s*$).+", message = "tgId must not be blank")
    @Size(max = 127, message = "tgId must be at most 127 characters")
    private String tgId;

    @Pattern(regexp = "^(?!\\s*$).+", message = "photo must not be blank")
    @Size(max = 1024, message = "photo must be at most 1024 characters")
    private String photo;

    @Pattern(regexp = "^(?!\\s*$).+", message = "password must not be blank")
    @Size(min = 6, max = 127, message = "password must be between 6 and 127 characters")
    private String password;

    

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBio() {
        return this.bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPhoto() {
        return this.photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTgId() {
        return this.tgId;
    }

    public void setTgId(String tgId) {
        this.tgId = tgId;
    }
}
