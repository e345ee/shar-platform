package com.course.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassJoinRequestDto {

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 1, max = 63, message = "Name must be between 1 and 63 characters")
    private String name;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    @Size(min = 1, max = 127, message = "Email must be between 1 and 127 characters")
    private String email;

    @Size(max = 127, message = "Telegram ID must be up to 127 characters")
    private String tgId;

    @NotBlank(message = "Class code cannot be blank")
    @Size(min = 8, max = 8, message = "Class code must be exactly 8 characters")
    private String classCode;
}
