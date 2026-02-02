package com.course.service;

import com.course.entity.User;
import com.course.exception.ForbiddenOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;

    /**
     * Resolve currently authenticated user as an ENTITY.
     */
    @Transactional(readOnly = true)
    public User getCurrentUserEntity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;

        if (username == null || username.isBlank()) {
            throw new ForbiddenOperationException("Unauthenticated");
        }

        return userService.getUserEntityByUsernameOrEmail(username);
    }
}
