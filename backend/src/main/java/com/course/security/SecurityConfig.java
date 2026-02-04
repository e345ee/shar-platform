package com.course.security;

import com.course.entity.RoleName;
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

                        .requestMatchers("/api/roles/**").hasRole(RoleName.ADMIN.name())

                        .requestMatchers(HttpMethod.POST, "/api/users/methodists/*/teachers").hasRole(RoleName.METHODIST.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/users/methodists/*/teachers/*").hasRole(RoleName.METHODIST.name())

                        .requestMatchers(HttpMethod.GET, "/api/users/me").hasAnyRole(RoleName.ADMIN.name(), RoleName.TEACHER.name(), RoleName.METHODIST.name(), RoleName.STUDENT.name())
                        .requestMatchers(HttpMethod.PUT, "/api/users/me").hasAnyRole(RoleName.ADMIN.name(), RoleName.TEACHER.name(), RoleName.METHODIST.name(), RoleName.STUDENT.name())
                        .requestMatchers(HttpMethod.PUT, "/api/users/me/password").hasAnyRole(RoleName.ADMIN.name(), RoleName.TEACHER.name(), RoleName.METHODIST.name(), RoleName.STUDENT.name())
                        .requestMatchers(HttpMethod.POST, "/api/users/me/avatar").hasAnyRole(RoleName.ADMIN.name(), RoleName.TEACHER.name(), RoleName.METHODIST.name(), RoleName.STUDENT.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/users/me/avatar").hasAnyRole(RoleName.ADMIN.name(), RoleName.TEACHER.name(), RoleName.METHODIST.name(), RoleName.STUDENT.name())

                        .requestMatchers(HttpMethod.GET, "/api/users/me/achievements").hasRole(RoleName.STUDENT.name())
                        .requestMatchers(HttpMethod.GET, "/api/users/me/achievements/page").hasRole(RoleName.STUDENT.name())

                        .requestMatchers("/api/users/**").hasRole(RoleName.ADMIN.name())

                        
                        .requestMatchers("/api/teachers/me/classes/**").hasAnyRole(RoleName.TEACHER.name(), RoleName.METHODIST.name())

                        .requestMatchers(HttpMethod.GET, "/api/courses/*/achievements/**").hasAnyRole(RoleName.ADMIN.name(), RoleName.TEACHER.name(), RoleName.METHODIST.name(), RoleName.STUDENT.name())
                        .requestMatchers(HttpMethod.GET, "/api/courses/*/lessons/**").hasAnyRole(RoleName.ADMIN.name(), RoleName.TEACHER.name(), RoleName.METHODIST.name(), RoleName.STUDENT.name())
                        .requestMatchers("/api/courses/**").hasRole(RoleName.METHODIST.name())
                        .requestMatchers("/api/classes/*/join-requests/**").hasAnyRole(RoleName.TEACHER.name(), RoleName.METHODIST.name())
                        .requestMatchers(HttpMethod.GET, "/api/classes/*/achievement-feed").hasAnyRole(RoleName.ADMIN.name(), RoleName.TEACHER.name(), RoleName.METHODIST.name(), RoleName.STUDENT.name())
                        .requestMatchers("/api/classes/**").hasRole(RoleName.METHODIST.name())
                        .requestMatchers("/api/courses/*/classes").hasRole(RoleName.METHODIST.name())

                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
