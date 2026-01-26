package com.course.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserDto {

    @NotNull(message = "Role ID cannot be null")
    private Integer roleId;

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 1, max = 63, message = "Name must be between 1 and 63 characters")
    private String name;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    @Size(min = 1, max = 127, message = "Email must be between 1 and 127 characters")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 1, max = 127, message = "Password must be between 1 and 127 characters")
    private String password;

    private String bio;
    private String photo;
    private String tgId;
}
