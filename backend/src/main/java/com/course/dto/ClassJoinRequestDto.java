package com.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassJoinRequestDto {
    private Integer id;
    private Integer classId;
    private String className;
    private String name;
    private String email;
    private String tgId;
    private LocalDateTime createdAt;
}
