package com.course.controller;

import com.course.dto.auth.AuthLoginRequest;
import com.course.dto.auth.AuthTokenResponse;
import com.course.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public AuthTokenResponse login(@Valid @RequestBody AuthLoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );

        UserDetails user = (UserDetails) auth.getPrincipal();
        String token = jwtService.generateAccessToken(user);
        return new AuthTokenResponse("Bearer", token, jwtService.getAccessTokenTtlSeconds());
    }
}
