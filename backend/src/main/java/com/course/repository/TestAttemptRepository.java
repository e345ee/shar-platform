package com.course.repository;

import com.course.entity.TestAttempt;
import com.course.entity.TestAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Integer> {

    List<TestAttempt> findAllByTest_IdAndStudent_IdOrderByAttemptNumberDesc(Integer testId, Integer studentId);

    List<TestAttempt> findAllByTest_IdOrderByCreatedAtDesc(Integer testId);

    Optional<TestAttempt> findFirstByTest_IdAndStudent_IdAndStatusOrderByAttemptNumberDesc(
            Integer testId,
            Integer studentId,
            TestAttemptStatus status
    );

    boolean existsByTest_IdAndStudent_IdAndStatus(Integer testId, Integer studentId, TestAttemptStatus status);

    int countByTest_IdAndStudent_Id(Integer testId, Integer studentId);
}
