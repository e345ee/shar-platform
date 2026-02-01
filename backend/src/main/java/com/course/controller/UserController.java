package com.course.controller;

import com.course.dto.PageDto;
import com.course.dto.UserDto;
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
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(dto));
    }

    /**
     * Admin business endpoint:
     * create a new METHODIST.
     *
     * Role is enforced on the backend; roleId in DTO (if provided) is ignored.
     */
    @PostMapping("/admin/methodists")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createMethodist(@Valid @RequestBody UserDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createMethodist(dto));
    }

    /**
     * Admin business endpoint:
     * delete only METHODIST users.
     */
    @DeleteMapping("/admin/methodists/{methodistId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMethodist(@PathVariable Integer methodistId) {
        userService.deleteMethodist(methodistId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin business endpoint:
     * change password of the currently authenticated ADMIN.
     *
     * Body is plain text (e.g. "newPassword") to avoid introducing extra DTOs.
     */
    @PutMapping("/admin/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeAdminPassword(@RequestBody String newPassword) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        userService.changeAdminPassword(username, newPassword);
        return ResponseEntity.noContent().build();
    }

    /**
     * Business endpoint (no auth yet):
     * METHODIST can register a new TEACHER.
     *
     * Role is enforced on the backend.
     */
    @PostMapping("/methodists/{methodistId}/teachers")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<UserDto> createTeacherByMethodist(
            @PathVariable Integer methodistId,
            @Valid @RequestBody UserDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createTeacherByMethodist(methodistId, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<UserDto> getUserByName(@PathVariable String name) {
        return ResponseEntity.ok(userService.getUserByName(name));
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/paginated")
    public ResponseEntity<PageDto<UserDto>> getAllUsersPaginated(Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsersPaginated(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UserDto dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Business endpoint (no auth yet):
     * METHODIST can delete only TEACHER users.
     */
    @DeleteMapping("/methodists/{methodistId}/teachers/{teacherId}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<Void> deleteTeacherByMethodist(
            @PathVariable Integer methodistId,
            @PathVariable Integer teacherId) {
        userService.deleteTeacherByMethodist(methodistId, teacherId);
        return ResponseEntity.noContent().build();
    }
}
