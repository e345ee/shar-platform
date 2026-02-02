package com.course.service;

import com.course.dto.CreateRoleDto;
import com.course.dto.RoleDto;
import com.course.entity.Role;
import com.course.exception.DuplicateResourceException;
import com.course.exception.ResourceNotFoundException;
import com.course.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService Tests")
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    private Role testRole;
    private CreateRoleDto createRoleDto;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(1);
        testRole.setRolename("ADMIN");
        testRole.setDescription("Administrator role");

        createRoleDto = new CreateRoleDto();
        createRoleDto.setRolename("ADMIN");
        createRoleDto.setDescription("Administrator role");
    }

    @Test
    @DisplayName("Should create role successfully")
    void testCreateRole() {
        when(roleRepository.existsByRolename("ADMIN")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);

        RoleDto result = roleService.createRole(createRoleDto);

        assertNotNull(result);
        assertEquals("ADMIN", result.getRolename());
        assertEquals("Administrator role", result.getDescription());
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    @DisplayName("Should throw exception when role already exists")
    void testCreateRoleDuplicate() {
        when(roleRepository.existsByRolename("ADMIN")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, 
                () -> roleService.createRole(createRoleDto));
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("Should get role by id")
    void testGetRoleById() {
        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));

        RoleDto result = roleService.getRoleById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("ADMIN", result.getRolename());
    }

    @Test
    @DisplayName("Should throw exception when role not found by id")
    void testGetRoleByIdNotFound() {
        when(roleRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
                () -> roleService.getRoleById(99));
    }

    @Test
    @DisplayName("Should get role by name")
    void testGetRoleByName() {
        when(roleRepository.findByRolename("ADMIN")).thenReturn(Optional.of(testRole));

        RoleDto result = roleService.getRoleByName("ADMIN");

        assertNotNull(result);
        assertEquals("ADMIN", result.getRolename());
    }

    @Test
    @DisplayName("Should throw exception when role not found by name")
    void testGetRoleByNameNotFound() {
        when(roleRepository.findByRolename("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
                () -> roleService.getRoleByName("NONEXISTENT"));
    }

    @Test
    @DisplayName("Should get all roles")
    void testGetAllRoles() {
        Role role2 = new Role();
        role2.setId(2);
        role2.setRolename("TEACHER");

        when(roleRepository.findAll()).thenReturn(Arrays.asList(testRole, role2));

        List<RoleDto> result = roleService.getAllRoles();

        assertEquals(2, result.size());
        assertEquals("ADMIN", result.get(0).getRolename());
        assertEquals("TEACHER", result.get(1).getRolename());
    }

    @Test
    @DisplayName("Should update role successfully")
    void testUpdateRole() {
        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);

        CreateRoleDto updateDto = new CreateRoleDto("ADMIN", "Updated description");
        RoleDto result = roleService.updateRole(1, updateDto);

        assertNotNull(result);
        assertEquals("ADMIN", result.getRolename());
    }

    @Test
    @DisplayName("Should throw exception when updating to duplicate rolename")
    void testUpdateRoleDuplicate() {
        Role existingRole = new Role();
        existingRole.setId(1);
        existingRole.setRolename("ADMIN");

        Role anotherRole = new Role();
        anotherRole.setId(2);
        anotherRole.setRolename("TEACHER");

        when(roleRepository.findById(1)).thenReturn(Optional.of(existingRole));
        when(roleRepository.existsByRolename("TEACHER")).thenReturn(true);

        CreateRoleDto updateDto = new CreateRoleDto("TEACHER", "Description");

        assertThrows(DuplicateResourceException.class, 
                () -> roleService.updateRole(1, updateDto));
    }

    @Test
    @DisplayName("Should delete role successfully")
    void testDeleteRole() {
        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));

        assertDoesNotThrow(() -> roleService.deleteRole(1));
        verify(roleRepository, times(1)).delete(testRole);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent role")
    void testDeleteRoleNotFound() {
        when(roleRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
                () -> roleService.deleteRole(99));
    }
}
