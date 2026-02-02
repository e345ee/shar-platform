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
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/join-requests").permitAll()

                        .requestMatchers("/api/roles/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/users/methodists/*/teachers").hasRole("METHODIST")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/methodists/*/teachers/*").hasRole("METHODIST")

                        .requestMatchers(HttpMethod.GET, "/api/users/me").hasAnyRole("ADMIN", "TEACHER", "METHODIST", "STUDENT")
                        .requestMatchers(HttpMethod.PUT, "/api/users/me").hasAnyRole("ADMIN", "TEACHER", "METHODIST", "STUDENT")
                        .requestMatchers(HttpMethod.PUT, "/api/users/me/password").hasAnyRole("ADMIN", "TEACHER", "METHODIST", "STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/users/me/avatar").hasAnyRole("ADMIN", "TEACHER", "METHODIST", "STUDENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/me/avatar").hasAnyRole("ADMIN", "TEACHER", "METHODIST", "STUDENT")

                        .requestMatchers(HttpMethod.GET, "/api/users/me/achievements").hasRole("STUDENT")

                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        
                        .requestMatchers("/api/teachers/me/classes/**").hasAnyRole("TEACHER", "METHODIST")

                        .requestMatchers(HttpMethod.GET, "/api/courses/*/achievements/**").hasAnyRole("ADMIN", "TEACHER", "METHODIST", "STUDENT")
                        .requestMatchers("/api/courses/**").hasRole("METHODIST")
                        .requestMatchers("/api/classes/*/join-requests/**").hasAnyRole("TEACHER", "METHODIST")
                        .requestMatchers("/api/classes/**").hasRole("METHODIST")
                        .requestMatchers("/api/courses/*/classes").hasRole("METHODIST")

                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
