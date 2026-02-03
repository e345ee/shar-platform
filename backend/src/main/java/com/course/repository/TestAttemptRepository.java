package com.course.repository;

import com.course.entity.TestAttempt;
import com.course.entity.TestAttemptStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Teacher sees only attempts of students that are in teacher's classes within the same course.
     */
    @Query("select ta from TestAttempt ta " +
            "where ta.test.id = :testId " +
            "and exists (select 1 from ClassStudent cs " +
            "where cs.student.id = ta.student.id " +
            "and cs.studyClass.teacher.id = :teacherId " +
            "and cs.studyClass.course.id = :courseId) " +
            "order by ta.createdAt desc")
    List<TestAttempt> findAllByTestIdForTeacher(
            @Param("testId") Integer testId,
            @Param("teacherId") Integer teacherId,
            @Param("courseId") Integer courseId
    );
}
