package com.course.service;

import com.course.dto.PageDto;
import com.course.dto.RoleDto;
import com.course.entity.Role;
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

    public RoleDto createRole(RoleDto dto) {
        if (roleRepository.existsByRolename(dto.getRolename())) {
            throw new DuplicateResourceException("Role with name '" + dto.getRolename() + "' already exists");
        }

        Role role = new Role();
        role.setRolename(dto.getRolename());
        role.setDescription(dto.getDescription());

        Role savedRole = roleRepository.save(role);
        return convertToDto(savedRole);
    }

    @Transactional(readOnly = true)
    public RoleDto getRoleById(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role with id " + id + " not found"));
        return convertToDto(role);
    }

    @Transactional(readOnly = true)
    public RoleDto getRoleByName(String rolename) {
        Role role = roleRepository.findByRolename(rolename)
                .orElseThrow(() -> new ResourceNotFoundException("Role with name '" + rolename + "' not found"));
        return convertToDto(role);
    }

    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageDto<RoleDto> getAllRolesPaginated(Pageable pageable) {
        Page<Role> page = roleRepository.findAll(pageable);
        return convertPageToPageDto(page);
    }

    public RoleDto updateRole(Integer id, RoleDto dto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role with id " + id + " not found"));

        if (!role.getRolename().equals(dto.getRolename()) && roleRepository.existsByRolename(dto.getRolename())) {
            throw new DuplicateResourceException("Role with name '" + dto.getRolename() + "' already exists");
        }

        role.setRolename(dto.getRolename());
        role.setDescription(dto.getDescription());

        Role updatedRole = roleRepository.save(role);
        return convertToDto(updatedRole);
    }

    public void deleteRole(Integer id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role with id " + id + " not found"));
        roleRepository.delete(role);
    }

    private RoleDto convertToDto(Role role) {
        return new RoleDto(role.getId(), role.getRolename(), role.getDescription());
    }

    private PageDto<RoleDto> convertPageToPageDto(Page<Role> page) {
        return new PageDto<>(
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
