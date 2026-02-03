package com.course.repository;

import com.course.entity.TestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestQuestionRepository extends JpaRepository<TestQuestion, Integer> {

    List<TestQuestion> findAllByTest_IdOrderByOrderIndexAsc(Integer testId);

    int countByTest_Id(Integer testId);

    boolean existsByTest_IdAndOrderIndex(Integer testId, Integer orderIndex);
}
