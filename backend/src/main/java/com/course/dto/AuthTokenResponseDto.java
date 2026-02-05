package com.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthTokenResponseDto {

    private String tokenType;
    private String accessToken;
    private long expiresInSeconds;
}
