package com.course.repository;

import com.course.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("RoleRepository Tests")
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    private Role testRole;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setRolename("ADMIN");
        testRole.setDescription("Administrator role");
    }

    @Test
    @DisplayName("Should save role successfully")
    void testSaveRole() {
        Role saved = roleRepository.save(testRole);
        
        assertNotNull(saved.getId());
        assertEquals("ADMIN", saved.getRolename());
        assertEquals("Administrator role", saved.getDescription());
    }

    @Test
    @DisplayName("Should find role by rolename")
    void testFindByRolename() {
        roleRepository.save(testRole);
        
        Optional<Role> found = roleRepository.findByRolename("ADMIN");
        
        assertTrue(found.isPresent());
        assertEquals("ADMIN", found.get().getRolename());
    }

    @Test
    @DisplayName("Should return empty when rolename not found")
    void testFindByRolenameNotFound() {
        Optional<Role> found = roleRepository.findByRolename("NONEXISTENT");
        
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should check if rolename exists")
    void testExistsByRolename() {
        roleRepository.save(testRole);
        
        assertTrue(roleRepository.existsByRolename("ADMIN"));
        assertFalse(roleRepository.existsByRolename("TEACHER"));
    }

    @Test
    @DisplayName("Should find role by id")
    void testFindById() {
        Role saved = roleRepository.save(testRole);
        
        Optional<Role> found = roleRepository.findById(saved.getId());
        
        assertTrue(found.isPresent());
        assertEquals("ADMIN", found.get().getRolename());
    }

    @Test
    @DisplayName("Should update role")
    void testUpdateRole() {
        Role saved = roleRepository.save(testRole);
        saved.setDescription("Updated description");
        
        Role updated = roleRepository.save(saved);
        
        assertEquals("Updated description", updated.getDescription());
    }

    @Test
    @DisplayName("Should delete role")
    void testDeleteRole() {
        Role saved = roleRepository.save(testRole);
        Integer id = saved.getId();
        
        roleRepository.delete(saved);
        
        assertFalse(roleRepository.findById(id).isPresent());
    }
}
