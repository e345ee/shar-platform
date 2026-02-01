package com.course.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt is the standard password hashing algorithm in Spring Security
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API: disable CSRF and do not create HTTP sessions
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Public endpoint
                        .requestMatchers("/api/health/**").permitAll()

                        // Role management: only ADMIN
                        .requestMatchers("/api/roles/**").hasRole("ADMIN")

                        // Methodist operations over teachers
                        .requestMatchers(HttpMethod.POST, "/api/users/methodists/*/teachers").hasRole("METHODIST")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/methodists/*/teachers/*").hasRole("METHODIST")

                        // Users CRUD: only ADMIN
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                // Simple authentication mechanism for coursework: HTTP Basic
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
