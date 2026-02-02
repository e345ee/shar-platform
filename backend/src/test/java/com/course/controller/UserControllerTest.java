package com.course.controller;

import com.course.dto.UserDto;
import com.course.exception.DuplicateResourceException;
import com.course.exception.ResourceNotFoundException;
import com.course.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserDto testUserDto;
    private UserDto createUserDto;

    @BeforeEach
    void setUp() {
        testUserDto = new UserDto(1, 1, "John Doe", "john@example.com", "A student", "photo.jpg", "john_doe");
        
        createUserDto = new UserDto(1, 1, "John Doe", "john@example.com", "A student", "photo.jpg", "john_doe");
    }

    @Test
    @DisplayName("Should create user and return 201")
    void testCreateUser() throws Exception {
        when(userService.createUser(any(UserDto.class))).thenReturn(testUserDto);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"));

        verify(userService, times(1)).createUser(any(UserDto.class));
    }

    @Test
    @DisplayName("Should get user by id and return 200")
    void testGetUserById() throws Exception {
        when(userService.getUserById(1)).thenReturn(testUserDto);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"));

        verify(userService, times(1)).getUserById(1);
    }

    @Test
    @DisplayName("Should return 404 when user not found by id")
    void testGetUserByIdNotFound() throws Exception {
        when(userService.getUserById(99))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get user by email and return 200")
    void testGetUserByEmail() throws Exception {
        when(userService.getUserByEmail("john@example.com")).thenReturn(testUserDto);

        mockMvc.perform(get("/api/users/email/john@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(userService, times(1)).getUserByEmail("john@example.com");
    }

    @Test
    @DisplayName("Should get user by name and return 200")
    void testGetUserByName() throws Exception {
        when(userService.getUserByName("John Doe")).thenReturn(testUserDto);

        mockMvc.perform(get("/api/users/name/John Doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"));

        verify(userService, times(1)).getUserByName("John Doe");
    }

    @Test
    @DisplayName("Should get all users and return 200")
    void testGetAllUsers() throws Exception {
        UserDto user2 = new UserDto(2, 1, "Jane Doe", "jane@example.com", "A teacher", null, "jane_doe");
        when(userService.getAllUsers()).thenReturn(Arrays.asList(testUserDto, user2));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[1].name").value("Jane Doe"));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("Should update user and return 200")
    void testUpdateUser() throws Exception {
        UserDto updateDto = new UserDto(1, 1, "John Doe", "john@example.com", null, null, null);
        
        when(userService.updateUser(eq(1), any(UserDto.class))).thenReturn(testUserDto);

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(userService, times(1)).updateUser(eq(1), any(UserDto.class));
    }

    @Test
    @DisplayName("Should delete user and return 204")
    void testDeleteUser() throws Exception {
        doNothing().when(userService).deleteUser(1);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(1);
    }

    @Test
    @DisplayName("Should return 409 when creating duplicate user email")
    void testCreateDuplicateUserEmail() throws Exception {
        when(userService.createUser(any(UserDto.class)))
                .thenThrow(new DuplicateResourceException("Email already exists"));

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserDto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should return 400 when email is invalid")
    void testCreateUserInvalidEmail() throws Exception {
        UserDto invalidDto = new UserDto(1, 1, "John Doe", "invalid-email", null, null, null);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when name is blank")
    void testCreateUserBlankName() throws Exception {
        UserDto invalidDto = new UserDto(1, 1, "", "john@example.com", null, null, null);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }
}
