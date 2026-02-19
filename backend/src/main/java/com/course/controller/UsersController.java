package com.course.controller;
import com.course.dto.auth.ChangeAdminPasswordRequest;
import com.course.dto.auth.UserRegisterRequest;
import com.course.dto.common.PageResponse;
import com.course.dto.user.UserResponse;
import com.course.dto.user.UserUpsertRequest;
import com.course.entity.User;
import com.course.exception.ForbiddenOperationException;
import com.course.service.AuthService;
import com.course.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Управление пользователями", description = "API для управления пользователями системы")
public class UsersController {

    private final UserService userService;
    private final AuthService authService;

    //****************************************** Админ ****************************************//
    @Operation(
            summary = "Регистрация методиста",
            description = "Регистрация нового методиста. Создает пользователя с обязательными полями: имя, email, пароль и опциональным полем tgId."
    )
    @PostMapping("/methodists")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createMethodist(@Valid @RequestBody UserRegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createMethodist(req));
    }

    @DeleteMapping("/methodists/{methodistId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMethodist(@PathVariable Integer methodistId) {
        userService.deleteMethodist(methodistId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/admin/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeAdminPassword(@Valid @RequestBody ChangeAdminPasswordRequest dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        userService.changeAdminPassword(username, dto.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/methodists")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> listMethodists() {
        return ResponseEntity.ok(userService.getAllMethodists());
    }

    //****************************************** Методист ****************************************//
    @Operation(
            summary = "Регистрация преподавателя",
            description = "Регистрация нового преподавателя методистом. Создает пользователя с обязательными полями: имя, email, пароль и опциональным полем tgId."
    )
    @PostMapping("/teachers")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<UserResponse> createTeacher(@Valid @RequestBody UserRegisterRequest req) {
        User current = authService.getCurrentUserEntity();
        if (current == null || current.getId() == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createTeacherByMethodist(current.getId(), req));
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

    // Не нужно! В uml нету такого функционала!
//    @PostMapping("/teachers/{teacherId}/restore")
//    @PreAuthorize("hasRole('METHODIST')")
//    public ResponseEntity<Void> restoreTeacher(@PathVariable Integer teacherId) {
//        User current = authService.getCurrentUserEntity();
//        if (current == null || current.getId() == null) {
//            throw new ForbiddenOperationException("Unauthenticated");
//        }
//        userService.restoreTeacherByMethodist(current.getId(), teacherId);
//        return ResponseEntity.noContent().build();
//    }

    @GetMapping("/teachers")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<PageResponse<UserResponse>> listMyTeachers(Pageable pageable) {
        User current = authService.getCurrentUserEntity();
        if (current == null || current.getId() == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }
        return ResponseEntity.ok(userService.listTeachersByMethodist(current.getId(), pageable));
    }

    //****************************************** Методист ****************************************//




    @Operation(
            summary = "Регистрация студента",
            description = "Регистрация нового студента администратором или методистом. Создает пользователя с обязательными полями: имя, email, пароль и опциональным полем tgId."
    )
    @PostMapping("/students")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<UserResponse> createStudent(@Valid @RequestBody UserRegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createStudent(req));
    }



//    @Operation(
//            summary = "Создание пользователя с указанием роли",
//            description = "Универсальный метод создания пользователя с любой ролью. Требует указания roleId. Создает пользователя с обязательными полями: имя, email, пароль и опциональным полем tgId."
//    )
//    @PostMapping
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRegisterRequest req) {
//        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createAdmin(req));
//    }

//    @PutMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<UserResponse> updateUser(@PathVariable Integer id, @Valid @RequestBody UserUpsertRequest dto) {
//        return ResponseEntity.ok(userService.updateUser(id, dto));
//    }
//
//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
//        userService.deleteUser(id);
//        return ResponseEntity.noContent().build();
//    }



    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','METHODIST','TEACHER')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyRole('ADMIN','METHODIST','TEACHER')")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/name/{name}")
    @PreAuthorize("hasAnyRole('ADMIN','METHODIST','TEACHER')")
    public ResponseEntity<UserResponse> getUserByName(@PathVariable String name) {
        return ResponseEntity.ok(userService.getUserByName(name));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','METHODIST')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN','METHODIST')")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsersPaginated(Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsersPaginated(pageable));
    }
}
