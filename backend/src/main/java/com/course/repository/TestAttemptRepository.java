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

    Optional<TestAttempt> findFirstByTest_IdAndStudent_IdAndStatusInOrderByAttemptNumberDesc(
            Integer testId,
            Integer studentId,
            List<TestAttemptStatus> status
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

    @Query("select ta from TestAttempt ta where ta.test.lesson.course.id = :courseId order by ta.createdAt desc")
    List<TestAttempt> findAllByCourseIdOrderByCreatedAtDesc(@Param("courseId") Integer courseId);

    @Query(value = """
            SELECT DISTINCT
              ta.id AS attemptId,
              ta.test_id AS testId,
              t.lesson_id AS lessonId,
              l.course_id AS courseId,
              c.id AS classId,
              c.name AS className,
              u.id AS studentId,
              u.name AS studentName,
              (
                SELECT COUNT(*)
                FROM test_attempt_answers taa
                JOIN test_questions tq ON tq.id = taa.question_id
                WHERE taa.attempt_id = ta.id
                  AND tq.question_type = 'OPEN'
                  AND taa.graded_at IS NULL
              ) AS ungradedOpenCount,
              ta.submitted_at AS submittedAt
            FROM test_attempts ta
            JOIN tests t ON t.id = ta.test_id
            JOIN lessons l ON l.id = t.lesson_id
            JOIN users u ON u.id = ta.student_id
            JOIN class_students cs ON cs.student_id = ta.student_id
            JOIN classes c ON c.id = cs.class_id AND c.course_id = l.course_id
            WHERE ta.status = 'SUBMITTED'
              AND c.teacher_id = :teacherId
              AND (:courseId IS NULL OR l.course_id = :courseId)
              AND (:testId IS NULL OR ta.test_id = :testId)
              AND (:classId IS NULL OR c.id = :classId)
              AND EXISTS (
                SELECT 1
                FROM test_attempt_answers taa2
                JOIN test_questions tq2 ON tq2.id = taa2.question_id
                WHERE taa2.attempt_id = ta.id
                  AND tq2.question_type = 'OPEN'
                  AND taa2.graded_at IS NULL
              )
            ORDER BY ta.submitted_at DESC NULLS LAST, ta.id DESC
            """, nativeQuery = true)
    List<PendingAttemptProjection> findPendingAttemptsForTeacher(
            @Param("teacherId") Integer teacherId,
            @Param("courseId") Integer courseId,
            @Param("testId") Integer testId,
            @Param("classId") Integer classId
    );
}
