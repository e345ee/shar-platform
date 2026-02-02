package com.course.init;

import com.course.entity.Role;
import com.course.entity.User;
import com.course.repository.RoleRepository;
import com.course.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the default ADMIN on the very first start of a fresh database.
 *
 * Requirements:
 * - login: admin
 * - password: admin
 *
 * We create an admin with name="admin" and email="admin@example.com".
 * Authentication allows both email or name as username.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ROLE_ADMIN = "ADMIN";

    @Override
    @Transactional
    public void run(String... args) {
        Role adminRole = roleRepository.findByRolename(ROLE_ADMIN)
                .orElseGet(() -> {
                    log.warn("Role '{}' not found. Creating it automatically.", ROLE_ADMIN);
                    Role r = new Role();
                    r.setRolename(ROLE_ADMIN);
                    r.setDescription("Администратор системы");
                    return roleRepository.save(r);
                });

        boolean adminExists = userRepository.existsByName("admin") || userRepository.existsByEmail("admin@example.com");
        if (adminExists) {
            return;
        }

        User admin = new User();
        admin.setRole(adminRole);
        admin.setName("admin");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("admin"));

        userRepository.save(admin);
        log.info("Default ADMIN created (login: admin, password: admin)");
    }
}
