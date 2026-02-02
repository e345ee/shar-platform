package com.course.repository;

import com.course.entity.ClassJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassJoinRequestRepository extends JpaRepository<ClassJoinRequest, Integer> {

    List<ClassJoinRequest> findAllByStudyClassIdOrderByCreatedAtDesc(Integer classId);

    Optional<ClassJoinRequest> findByIdAndStudyClassId(Integer id, Integer classId);

    boolean existsByStudyClassIdAndEmailIgnoreCase(Integer classId, String email);
}
