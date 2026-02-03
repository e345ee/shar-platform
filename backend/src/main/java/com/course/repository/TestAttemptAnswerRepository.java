package com.course.repository;

import com.course.entity.TestAttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestAttemptAnswerRepository extends JpaRepository<TestAttemptAnswer, Integer> {

    List<TestAttemptAnswer> findAllByAttempt_IdOrderByIdAsc(Integer attemptId);
}
