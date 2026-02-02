package com.course.repository;

import com.course.entity.Role;
import com.course.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setRolename("STUDENT");
        testRole.setDescription("Student role");
        testRole = roleRepository.save(testRole);

        testUser = new User();
        testUser.setRole(testRole);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPassword("password123");
        testUser.setBio("A student");
        testUser.setTgId("john_doe");
    }

    @Test
    @DisplayName("Should save user successfully")
    void testSaveUser() {
        User saved = userRepository.save(testUser);
        
        assertNotNull(saved.getId());
        assertEquals("John Doe", saved.getName());
        assertEquals("john@example.com", saved.getEmail());
    }

    @Test
    @DisplayName("Should find user by email")
    void testFindByEmail() {
        userRepository.save(testUser);
        
        Optional<User> found = userRepository.findByEmail("john@example.com");
        
        assertTrue(found.isPresent());
        assertEquals("John Doe", found.get().getName());
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void testFindByEmailNotFound() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");
        
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should find user by name")
    void testFindByName() {
        userRepository.save(testUser);
        
        Optional<User> found = userRepository.findByName("John Doe");
        
        assertTrue(found.isPresent());
        assertEquals("john@example.com", found.get().getEmail());
    }

    @Test
    @DisplayName("Should check if email exists")
    void testExistsByEmail() {
        userRepository.save(testUser);
        
        assertTrue(userRepository.existsByEmail("john@example.com"));
        assertFalse(userRepository.existsByEmail("jane@example.com"));
    }

    @Test
    @DisplayName("Should check if name exists")
    void testExistsByName() {
        userRepository.save(testUser);
        
        assertTrue(userRepository.existsByName("John Doe"));
        assertFalse(userRepository.existsByName("Jane Doe"));
    }

    @Test
    @DisplayName("Should check if tgId exists")
    void testExistsByTgId() {
        userRepository.save(testUser);
        
        assertTrue(userRepository.existsByTgId("john_doe"));
        assertFalse(userRepository.existsByTgId("jane_doe"));
    }

    @Test
    @DisplayName("Should find user by id")
    void testFindById() {
        User saved = userRepository.save(testUser);
        
        Optional<User> found = userRepository.findById(saved.getId());
        
        assertTrue(found.isPresent());
        assertEquals("John Doe", found.get().getName());
    }

    @Test
    @DisplayName("Should update user")
    void testUpdateUser() {
        User saved = userRepository.save(testUser);
        saved.setBio("Updated bio");
        
        User updated = userRepository.save(saved);
        
        assertEquals("Updated bio", updated.getBio());
    }

    @Test
    @DisplayName("Should delete user")
    void testDeleteUser() {
        User saved = userRepository.save(testUser);
        Integer id = saved.getId();
        
        userRepository.delete(saved);
        
        assertFalse(userRepository.findById(id).isPresent());
    }
}
