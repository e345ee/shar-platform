package com.course.controller;

import com.course.dto.CreateRoleDto;
import com.course.dto.RoleDto;
import com.course.exception.DuplicateResourceException;
import com.course.exception.ResourceNotFoundException;
import com.course.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoleController.class)
@DisplayName("RoleController Tests")
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoleService roleService;

    private RoleDto testRoleDto;
    private CreateRoleDto createRoleDto;

    @BeforeEach
    void setUp() {
        testRoleDto = new RoleDto(1, "ADMIN", "Administrator role");
        createRoleDto = new CreateRoleDto("ADMIN", "Administrator role");
    }

    @Test
    @DisplayName("Should create role and return 201")
    void testCreateRole() throws Exception {
        when(roleService.createRole(any(CreateRoleDto.class))).thenReturn(testRoleDto);

        mockMvc.perform(post("/api/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRoleDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rolename").value("ADMIN"));

        verify(roleService, times(1)).createRole(any(CreateRoleDto.class));
    }

    @Test
    @DisplayName("Should get role by id and return 200")
    void testGetRoleById() throws Exception {
        when(roleService.getRoleById(1)).thenReturn(testRoleDto);

        mockMvc.perform(get("/api/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rolename").value("ADMIN"));

        verify(roleService, times(1)).getRoleById(1);
    }

    @Test
    @DisplayName("Should return 404 when role not found")
    void testGetRoleByIdNotFound() throws Exception {
        when(roleService.getRoleById(99))
                .thenThrow(new ResourceNotFoundException("Role not found"));

        mockMvc.perform(get("/api/roles/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get role by name and return 200")
    void testGetRoleByName() throws Exception {
        when(roleService.getRoleByName("ADMIN")).thenReturn(testRoleDto);

        mockMvc.perform(get("/api/roles/name/ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolename").value("ADMIN"));

        verify(roleService, times(1)).getRoleByName("ADMIN");
    }

    @Test
    @DisplayName("Should get all roles and return 200")
    void testGetAllRoles() throws Exception {
        RoleDto role2 = new RoleDto(2, "TEACHER", "Teacher role");
        when(roleService.getAllRoles()).thenReturn(Arrays.asList(testRoleDto, role2));

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].rolename").value("ADMIN"))
                .andExpect(jsonPath("$[1].rolename").value("TEACHER"));

        verify(roleService, times(1)).getAllRoles();
    }

    @Test
    @DisplayName("Should update role and return 200")
    void testUpdateRole() throws Exception {
        CreateRoleDto updateDto = new CreateRoleDto("ADMIN", "Updated description");
        when(roleService.updateRole(eq(1), any(CreateRoleDto.class))).thenReturn(testRoleDto);

        mockMvc.perform(put("/api/roles/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(roleService, times(1)).updateRole(eq(1), any(CreateRoleDto.class));
    }

    @Test
    @DisplayName("Should delete role and return 204")
    void testDeleteRole() throws Exception {
        doNothing().when(roleService).deleteRole(1);

        mockMvc.perform(delete("/api/roles/1"))
                .andExpect(status().isNoContent());

        verify(roleService, times(1)).deleteRole(1);
    }

    @Test
    @DisplayName("Should return 409 when creating duplicate role")
    void testCreateDuplicateRole() throws Exception {
        when(roleService.createRole(any(CreateRoleDto.class)))
                .thenThrow(new DuplicateResourceException("Role already exists"));

        mockMvc.perform(post("/api/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRoleDto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should return 400 when rolename is blank")
    void testCreateRoleBlankRolename() throws Exception {
        CreateRoleDto invalidDto = new CreateRoleDto("", "Description");

        mockMvc.perform(post("/api/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }
}
