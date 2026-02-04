package com.course.service;

import com.course.dto.CreateUserDto;
import com.course.dto.UserDto;
import com.course.entity.Role;
import com.course.entity.RoleName;
import com.course.entity.User;
import com.course.exception.DuplicateResourceException;
import com.course.exception.ResourceNotFoundException;
import com.course.repository.RoleRepository;
import com.course.repository.UserRepository;
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
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role testRole;
    private CreateUserDto createUserDto;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(1);
        testRole.setRolename(RoleName.STUDENT);

        testUser = new User();
        testUser.setId(1);
        testUser.setRole(testRole);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPassword("password123");
        testUser.setBio("A student");
        testUser.setTgId("john_doe");

        createUserDto = new CreateUserDto();
        createUserDto.setRoleId(1);
        createUserDto.setName("John Doe");
        createUserDto.setEmail("john@example.com");
        createUserDto.setPassword("password123");
        createUserDto.setBio("A student");
        createUserDto.setTgId("john_doe");
    }

    @Test
    @DisplayName("Should create user successfully")
    void testCreateUser() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByName("John Doe")).thenReturn(false);
        when(userRepository.existsByTgId("john_doe")).thenReturn(false);
        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userService.createUser(createUserDto);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals("john@example.com", result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void testCreateUserDuplicateEmail() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, 
                () -> userService.createUser(createUserDto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when name already exists")
    void testCreateUserDuplicateName() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByName("John Doe")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, 
                () -> userService.createUser(createUserDto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when tgId already exists")
    void testCreateUserDuplicateTgId() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByName("John Doe")).thenReturn(false);
        when(userRepository.existsByTgId("john_doe")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, 
                () -> userService.createUser(createUserDto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when role not found")
    void testCreateUserRoleNotFound() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByName("John Doe")).thenReturn(false);
        when(userRepository.existsByTgId("john_doe")).thenReturn(false);
        when(roleRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
                () -> userService.createUser(createUserDto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should get user by id")
    void testGetUserById() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        UserDto result = userService.getUserById(1);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    @DisplayName("Should throw exception when user not found by id")
    void testGetUserByIdNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
                () -> userService.getUserById(99));
    }

    @Test
    @DisplayName("Should get user by email")
    void testGetUserByEmail() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

        UserDto result = userService.getUserByEmail("john@example.com");

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
    }

    @Test
    @DisplayName("Should get user by name")
    void testGetUserByName() {
        when(userRepository.findByName("John Doe")).thenReturn(Optional.of(testUser));

        UserDto result = userService.getUserByName("John Doe");

        assertNotNull(result);
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    @DisplayName("Should get all users")
    void testGetAllUsers() {
        User user2 = new User();
        user2.setId(2);
        user2.setName("Jane Doe");
        user2.setEmail("jane@example.com");
        user2.setRole(testRole);

        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));

        List<UserDto> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals("John Doe", result.get(0).getName());
        assertEquals("Jane Doe", result.get(1).getName());
    }

    @Test
    @DisplayName("Should update user successfully")
    void testUpdateUser() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(roleRepository.findById(1)).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userService.updateUser(1, createUserDto);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
    }

    @Test
    @DisplayName("Should delete user successfully")
    void testDeleteUser() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        assertDoesNotThrow(() -> userService.deleteUser(1));
        verify(userRepository, times(1)).delete(testUser);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent user")
    void testDeleteUserNotFound() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, 
                () -> userService.deleteUser(99));
    }
}
