package com.course.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Integer id;

    @Min(value = 1, message = "roleId must be a positive integer")
    private Integer roleId;

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 1, max = 63, message = "name must be between 1 and 63 characters")
    private String name;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    @Size(min = 3, max = 127, message = "email must be between 3 and 127 characters")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 127, message = "password must be between 6 and 127 characters")
    private String password;

    @Pattern(regexp = "^(?!\\s*$).+", message = "bio must not be blank")
    @Size(max = 2048, message = "bio must be at most 2048 characters")
    private String bio;

    @Pattern(regexp = "^(?!\\s*$).+", message = "photo must not be blank")
    @Size(max = 1024, message = "photo must be at most 1024 characters")
    private String photo;

    @Pattern(regexp = "^(?!\\s*$).+", message = "tgId must not be blank")
    @Size(max = 127, message = "tgId must be at most 127 characters")
    private String tgId;

    

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getRoleId() {
        return this.roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getTgId() {
        return this.tgId;
    }

    public void setTgId(String tgId) {
        this.tgId = tgId;
    }

}
