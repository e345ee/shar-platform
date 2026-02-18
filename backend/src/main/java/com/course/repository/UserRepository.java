package com.course.repository;

import com.course.entity.RoleName;
import com.course.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Optional<User> findByName(String name);
    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByNameAndDeletedFalse(String name);
    boolean existsByEmail(String email);
    boolean existsByName(String name);
    boolean existsByTgId(String tgId);
    List<User> findAllByRole_RolenameAndDeletedFalseOrderByNameAsc(RoleName roleName);

    @Query(value = """
        SELECT register_user(
            :name, 
            :email, 
            :hashedPassword, 
            CAST(:roleName AS role_name),
            CASE WHEN :tgId IS NULL OR :tgId = '' THEN NULL ELSE :tgId END
        )
        """, nativeQuery = true)
    Integer registerUser(
            @Param("name") String name,
            @Param("email") String email,
            @Param("hashedPassword") String hashedPassword,
            @Param("roleName") String roleName,
            @Param("tgId") String tgId
    );
}
