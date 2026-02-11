package com.course.dto.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;





@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassJoinRequestByCodeRequest {

    @NotBlank(message = "Class code cannot be blank")
    @Size(min = 8, max = 8, message = "Class code must be exactly 8 characters")
    private String classCode;
}
