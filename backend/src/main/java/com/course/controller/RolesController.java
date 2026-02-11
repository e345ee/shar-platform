package com.course.controller;

import com.course.dto.common.PageResponse;
import com.course.dto.role.RoleResponse;
import com.course.dto.role.RoleUpsertRequest;
import com.course.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolesController {

    private final RoleService roleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleUpsertRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<RoleResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    @GetMapping("/name/{rolename}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<RoleResponse> getByName(@PathVariable String rolename) {
        return ResponseEntity.ok(roleService.getRoleByName(rolename));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<List<RoleResponse>> getAll() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<PageResponse<RoleResponse>> getAllPaginated(Pageable pageable) {
        return ResponseEntity.ok(roleService.getAllRolesPaginated(pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleResponse> update(@PathVariable Integer id, @Valid @RequestBody RoleUpsertRequest dto) {
        return ResponseEntity.ok(roleService.updateRole(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
