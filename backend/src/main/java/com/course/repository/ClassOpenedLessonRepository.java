package com.course.repository;

import com.course.entity.ClassOpenedLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassOpenedLessonRepository extends JpaRepository<ClassOpenedLesson, Integer> {

    boolean existsByStudyClass_IdAndLesson_Id(Integer classId, Integer lessonId);

    @Query("""
            SELECT CASE WHEN COUNT(col.id) > 0 THEN TRUE ELSE FALSE END
            FROM ClassOpenedLesson col
            JOIN ClassStudent cs ON cs.studyClass.id = col.studyClass.id
            WHERE cs.student.id = :studentId
              AND col.lesson.id = :lessonId
            """)
    boolean isLessonOpenedForStudent(@Param("studentId") Integer studentId, @Param("lessonId") Integer lessonId);

    @Query("""
            SELECT DISTINCT col.lesson.id
            FROM ClassOpenedLesson col
            JOIN ClassStudent cs ON cs.studyClass.id = col.studyClass.id
            WHERE cs.student.id = :studentId
              AND col.studyClass.course.id = :courseId
            """)
    List<Integer> findOpenedLessonIdsForStudentInCourse(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    @Query("""
            SELECT DISTINCT col.studyClass.id
            FROM ClassOpenedLesson col
            WHERE col.lesson.id = :lessonId
            """)
    List<Integer> findOpenClassIdsByLessonId(@Param("lessonId") Integer lessonId);
}
