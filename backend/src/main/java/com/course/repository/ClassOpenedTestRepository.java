package com.course.repository;

import com.course.entity.ClassOpenedTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassOpenedTestRepository extends JpaRepository<ClassOpenedTest, Integer> {

    boolean existsByStudyClass_IdAndTest_Id(Integer classId, Integer testId);

    @Query("""
            SELECT CASE WHEN COUNT(cot.id) > 0 THEN TRUE ELSE FALSE END
            FROM ClassOpenedTest cot
            JOIN ClassStudent cs ON cs.studyClass.id = cot.studyClass.id
            WHERE cs.student.id = :studentId
              AND cot.test.id = :testId
            """)
    boolean isTestOpenedForStudent(@Param("studentId") Integer studentId, @Param("testId") Integer testId);

    @Query("""
            SELECT DISTINCT cot.test.id
            FROM ClassOpenedTest cot
            JOIN ClassStudent cs ON cs.studyClass.id = cot.studyClass.id
            WHERE cs.student.id = :studentId
              AND cot.test.lesson.id = :lessonId
            """)
    List<Integer> findOpenedTestIdsForStudentInLesson(@Param("studentId") Integer studentId, @Param("lessonId") Integer lessonId);

    @Query("""
            SELECT DISTINCT cot.test.id
            FROM ClassOpenedTest cot
            JOIN ClassStudent cs ON cs.studyClass.id = cot.studyClass.id
            WHERE cs.student.id = :studentId
              AND cot.test.course.id = :courseId
            """)
    List<Integer> findOpenedTestIdsForStudentInCourse(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);
}
