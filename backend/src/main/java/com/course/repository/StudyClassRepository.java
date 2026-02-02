package com.course.repository;

import com.course.entity.StudyClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyClassRepository extends JpaRepository<StudyClass, Integer> {
    List<StudyClass> findAllByCourseId(Integer courseId);

    List<StudyClass> findAllByTeacherId(Integer teacherId);

    List<StudyClass> findAllByCreatedById(Integer createdById);

    Optional<StudyClass> findByIdAndTeacherId(Integer id, Integer teacherId);

    Optional<StudyClass> findByIdAndCreatedById(Integer id, Integer createdById);

    boolean existsByJoinCode(String joinCode);

    Optional<StudyClass> findByJoinCode(String joinCode);
}
