package com.course.repository;

import com.course.entity.StudyClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyClassRepository extends JpaRepository<StudyClass, Integer> {
    List<StudyClass> findAllByCourseId(Integer courseId);
}
