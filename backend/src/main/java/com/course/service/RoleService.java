package com.course.service;

import com.course.dto.common.PageResponse;
import com.course.dto.role.RoleResponse;
import com.course.dto.role.RoleUpsertRequest;
import com.course.entity.Role;
import com.course.entity.RoleName;
import com.course.exception.DuplicateResourceException;
import com.course.exception.ResourceNotFoundException;
import com.course.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleResponse createRole(RoleUpsertRequest dto) {
        RoleName roleName = parseRoleName(dto.getRolename());
        if (roleRepository.existsByRolename(roleName)) {
            throw new DuplicateResourceException("Role with name '" + roleName.name() + "' already exists");
        }

        Role role = new Role();
        role.setRolename(roleName);
        role.setDescription(dto.getDescription());

        Role savedRole = roleRepository.save(role);
        return convertToDto(savedRole);
    }

    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role with id " + id + " not found"));
        return convertToDto(role);
    }

    @Transactional(readOnly = true)
    public RoleResponse getRoleByName(String rolename) {
        RoleName roleName = parseRoleName(rolename);
        Role role = roleRepository.findByRolename(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role with name '" + roleName.name() + "' not found"));
        return convertToDto(role);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<RoleResponse> getAllRolesPaginated(Pageable pageable) {
        Page<Role> page = roleRepository.findAll(pageable);
        return convertPageToPageDto(page);
    }

    public RoleResponse updateRole(Integer id, RoleUpsertRequest dto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role with id " + id + " not found"));

        RoleName newRoleName = parseRoleName(dto.getRolename());

        if (!role.getRolename().equals(newRoleName) && roleRepository.existsByRolename(newRoleName)) {
            throw new DuplicateResourceException("Role with name '" + newRoleName.name() + "' already exists");
        }

        role.setRolename(newRoleName);
        role.setDescription(dto.getDescription());

        Role updatedRole = roleRepository.save(role);
        return convertToDto(updatedRole);
    }

    public void deleteRole(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role with id " + id + " not found"));
        roleRepository.delete(role);
    }

    private RoleResponse convertToDto(Role role) {
        return new RoleResponse(role.getId(), role.getRolename() != null ? role.getRolename().name() : null, role.getDescription());
    }

    private RoleName parseRoleName(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Role name cannot be null");
        }
        try {
            return RoleName.valueOf(raw.trim().toUpperCase());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unknown role name: " + raw);
        }
    }

    private PageResponse<RoleResponse> convertPageToPageDto(Page<Role> page) {
        return new PageResponse<>(
                page.getContent().stream()
                        .map(this::convertToDto)
                        .collect(Collectors.toList()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst()
        );
    }
}
