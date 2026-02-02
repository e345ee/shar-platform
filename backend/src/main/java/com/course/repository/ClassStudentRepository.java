package com.course.repository;

import com.course.entity.ClassStudent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassStudentRepository extends JpaRepository<ClassStudent, Integer> {
    boolean existsByStudyClassIdAndStudentId(Integer classId, Integer studentId);
}
