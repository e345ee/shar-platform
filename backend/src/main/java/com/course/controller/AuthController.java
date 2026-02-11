package com.course.controller;

import com.course.dto.auth.AuthLoginRequest;
import com.course.dto.auth.AuthRefreshRequest;
import com.course.dto.auth.AuthTokenResponse;
import com.course.dto.auth.StudentRegisterRequest;
import com.course.dto.user.UserUpsertRequest;
import com.course.security.JwtService;
import com.course.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserService userService;

    private static final String REFRESH_COOKIE = "refresh_token";

    



    @PostMapping("/register")
    public ResponseEntity<AuthTokenResponse> register(@Valid @RequestBody StudentRegisterRequest req) {
        UserUpsertRequest dto = new UserUpsertRequest();
        dto.setName(req.getName());
        dto.setEmail(req.getEmail());
        dto.setPassword(req.getPassword());
        dto.setTgId(req.getTgId());

        userService.createStudent(dto);

        UserDetails user = userDetailsService.loadUserByUsername(req.getEmail());
        String token = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);

        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, refresh)
                .httpOnly(true)
                .path("/api/auth")
                .sameSite("Lax")
                .maxAge(jwtService.getRefreshTokenTtlSeconds())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthTokenResponse("Bearer", token, jwtService.getAccessTokenTtlSeconds()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody AuthLoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );

        UserDetails user = (UserDetails) auth.getPrincipal();
        String token = jwtService.generateAccessToken(user);

        String refresh = jwtService.generateRefreshToken(user);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, refresh)
                .httpOnly(true)
                .path("/api/auth")
                .sameSite("Lax")
                .maxAge(jwtService.getRefreshTokenTtlSeconds())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthTokenResponse("Bearer", token, jwtService.getAccessTokenTtlSeconds()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(@Valid @RequestBody(required = false) AuthRefreshRequest req, HttpServletRequest request) {
        String refreshToken = req != null ? req.getRefreshToken() : null;
        if (refreshToken == null || refreshToken.isBlank()) {
            Cookie[] cookies = request != null ? request.getCookies() : null;
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if (REFRESH_COOKIE.equals(c.getName())) {
                        refreshToken = c.getValue();
                        break;
                    }
                }
            }
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        String username;
        try {
            username = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }

        UserDetails user = userDetailsService.loadUserByUsername(username);
        if (!jwtService.isRefreshTokenValid(refreshToken, user)) {
            return ResponseEntity.status(401).build();
        }

        String newAccess = jwtService.generateAccessToken(user);
        String newRefresh = jwtService.generateRefreshToken(user);

        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, newRefresh)
                .httpOnly(true)
                .path("/api/auth")
                .sameSite("Lax")
                .maxAge(jwtService.getRefreshTokenTtlSeconds())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthTokenResponse("Bearer", newAccess, jwtService.getAccessTokenTtlSeconds()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .path("/api/auth")
                .sameSite("Lax")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }
}
