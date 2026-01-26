package com.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Integer id;
    private Integer roleId;
    private String name;
    private String email;
    private String bio;
    private String photo;
    private String tgId;
}
