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

                        // Public join requests (submitted by external service)
                        .requestMatchers(HttpMethod.POST, "/api/join-requests").permitAll()

                        // Role management: only ADMIN
                        .requestMatchers("/api/roles/**").hasRole("ADMIN")

                        // Methodist operations over teachers
                        .requestMatchers(HttpMethod.POST, "/api/users/methodists/*/teachers").hasRole("METHODIST")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/methodists/*/teachers/*").hasRole("METHODIST")

                        // Self-profile & avatar (teacher/methodist/student)
                        .requestMatchers(HttpMethod.GET, "/api/users/me").hasAnyRole("TEACHER", "METHODIST", "STUDENT")
                        .requestMatchers(HttpMethod.PUT, "/api/users/me").hasAnyRole("TEACHER", "METHODIST", "STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/users/me/avatar").hasAnyRole("TEACHER", "METHODIST", "STUDENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/me/avatar").hasAnyRole("TEACHER", "METHODIST", "STUDENT")

                        // Users CRUD: only ADMIN
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        
                        // Courses & classes: only METHODIST
                        .requestMatchers("/api/courses/**").hasRole("METHODIST")
                        // join requests management: TEACHER or METHODIST
                        .requestMatchers("/api/classes/*/join-requests/**").hasAnyRole("TEACHER", "METHODIST")
                        .requestMatchers("/api/classes/**").hasRole("METHODIST")
                        .requestMatchers("/api/courses/*/classes").hasRole("METHODIST")

                        .anyRequest().authenticated()
                )
                // Simple authentication mechanism for coursework: HTTP Basic
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
