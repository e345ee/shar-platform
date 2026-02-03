package com.course.repository;

import com.course.entity.TestAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestAnswerRepository extends JpaRepository<TestAnswer, Integer> {

    Optional<TestAnswer> findByAttempt_IdAndQuestion_Id(Integer attemptId, Integer questionId);

    List<TestAnswer> findAllByAttempt_Id(Integer attemptId);

    int countByAttempt_Id(Integer attemptId);
}
