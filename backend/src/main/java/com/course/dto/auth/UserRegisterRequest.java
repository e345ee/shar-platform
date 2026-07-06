package com.course.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterRequest {

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

    @Pattern(regexp = "^(?!\\s*$).+", message = "tgId must not be blank")
    @Size(max = 127, message = "tgId must be at most 127 characters")
    private String tgId;

    @Schema(description = "ID роли пользователя (опционально, используется только для универсального метода createUser)", example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Min(value = 1, message = "roleId must be a positive integer")
    private Integer roleId;
}

