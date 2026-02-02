package com.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single DTO for both requests (create/update) and responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {
    private Integer id;

    @NotBlank(message = "Role name cannot be blank")
    @Size(min = 1, max = 63, message = "Role name must be between 1 and 63 characters")
    private String rolename;

    private String description;
}
