package com.course.controller;

import com.course.dto.PageDto;
import com.course.dto.UserDto;
import com.course.entity.User;
import com.course.exception.ForbiddenOperationException;
import com.course.service.AuthService;
import com.course.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsersController {

    private final UserService userService;
    private final AuthService authService;

    // --- Admin: manage methodists ---

    @PostMapping("/methodists")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createMethodist(@Valid @RequestBody UserDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createMethodist(dto));
    }

    @DeleteMapping("/methodists/{methodistId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMethodist(@PathVariable Integer methodistId) {
        userService.deleteMethodist(methodistId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Keep legacy functionality: allow ADMIN to rotate own password (not via /me/password).
     * Body is a raw JSON string, e.g. "newPassword".
     */
    @PutMapping("/admin/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeAdminPassword(@RequestBody String newPassword) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        userService.changeAdminPassword(username, newPassword);
        return ResponseEntity.noContent().build();
    }

    // --- Methodist: manage teachers ---

    @PostMapping("/teachers")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<UserDto> createTeacher(@Valid @RequestBody UserDto dto) {
        User current = authService.getCurrentUserEntity();
        if (current == null || current.getId() == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createTeacherByMethodist(current.getId(), dto));
    }

    @DeleteMapping("/teachers/{teacherId}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Integer teacherId) {
        User current = authService.getCurrentUserEntity();
        if (current == null || current.getId() == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }
        userService.deleteTeacherByMethodist(current.getId(), teacherId);
        return ResponseEntity.noContent().build();
    }

    // --- Create student with unique Telegram ID ---

    @PostMapping("/students")
    @PreAuthorize("hasAnyRole('ADMIN','METHODIST')")
    public ResponseEntity<UserDto> createStudent(@Valid @RequestBody UserDto dto) {
        // Ignore roleId if sent, enforce STUDENT role in service.
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createStudent(dto));
    }

    // --- Generic user management (admin only) ---

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUser(@PathVariable Integer id, @Valid @RequestBody UserDto dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // --- Read/search users (admin/methodist/teacher) ---

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','METHODIST','TEACHER')")
    public ResponseEntity<UserDto> getUserById(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyRole('ADMIN','METHODIST','TEACHER')")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/name/{name}")
    @PreAuthorize("hasAnyRole('ADMIN','METHODIST','TEACHER')")
    public ResponseEntity<UserDto> getUserByName(@PathVariable String name) {
        return ResponseEntity.ok(userService.getUserByName(name));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','METHODIST')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN','METHODIST')")
    public ResponseEntity<PageDto<UserDto>> getAllUsersPaginated(Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsersPaginated(pageable));
    }
}
