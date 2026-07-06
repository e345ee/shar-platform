package com.course.dto.auth;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRefreshRequest {

    @Size(max = 4096)
    private String refreshToken;
}
