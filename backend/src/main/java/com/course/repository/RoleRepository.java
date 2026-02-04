package com.course.repository;

import com.course.entity.Role;
import com.course.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRolename(RoleName rolename);
    boolean existsByRolename(RoleName rolename);
}
