package com.course.repository;

import com.course.entity.Test;
import com.course.entity.TestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestRepository extends JpaRepository<Test, Integer> {

    boolean existsByLesson_Id(Integer lessonId);

    Optional<Test> findByLesson_Id(Integer lessonId);

    List<Test> findAllByLesson_Id(Integer lessonId);

    List<Test> findAllByLesson_IdAndStatus(Integer lessonId, TestStatus status);
}
