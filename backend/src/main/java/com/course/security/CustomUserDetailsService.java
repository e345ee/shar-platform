package com.course.security;

import com.course.entity.User;
import com.course.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Username can be either email OR name.
     *
     * This is convenient for demos like "admin" / "admin" while still
     * supporting email-based login.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .or(() -> userRepository.findByName(username))
                .orElseThrow(() -> new UsernameNotFoundException("User '" + username + "' not found"));

        if (user.getRole() == null || user.getRole().getRolename() == null) {
                throw new UsernameNotFoundException("User has no role assigned: " + username);
        }

        String roleName = user.getRole().getRolename().toUpperCase();
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + roleName));

        return org.springframework.security.core.userdetails.User
                // Keep the canonical username as email (if present)
                .withUsername(username)
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }
}
