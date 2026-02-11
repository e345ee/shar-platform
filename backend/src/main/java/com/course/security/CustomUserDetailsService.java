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

    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndDeletedFalse(username)
                .or(() -> userRepository.findByNameAndDeletedFalse(username))
                .orElseThrow(() -> new UsernameNotFoundException("User '" + username + "' not found"));

        if (user.getRole() == null || user.getRole().getRolename() == null) {
                throw new UsernameNotFoundException("User has no role assigned: " + username);
        }

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(user.getRole().getRolename().authority())
        );

        return org.springframework.security.core.userdetails.User
                
                .withUsername(username)
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }
}
