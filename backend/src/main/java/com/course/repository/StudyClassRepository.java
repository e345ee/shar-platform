package com.course.repository;

import com.course.entity.StudyClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudyClassRepository extends JpaRepository<StudyClass, Integer> {
    List<StudyClass> findAllByCourseId(Integer courseId);

    List<StudyClass> findAllByTeacherId(Integer teacherId);

    List<StudyClass> findAllByCreatedById(Integer createdById);

    @Query("select distinct cs.studyClass from ClassStudent cs where cs.student.id = :studentId order by cs.studyClass.name asc")
    List<StudyClass> findAllByStudentId(@Param("studentId") Integer studentId);

    Optional<StudyClass> findByIdAndTeacherId(Integer id, Integer teacherId);

    Optional<StudyClass> findByIdAndCreatedById(Integer id, Integer createdById);

    @Query("select cs.studyClass from ClassStudent cs where cs.studyClass.id = :id and cs.student.id = :studentId")
    Optional<StudyClass> findByIdAndStudentId(@Param("id") Integer id, @Param("studentId") Integer studentId);

    boolean existsByJoinCode(String joinCode);

    Optional<StudyClass> findByJoinCode(String joinCode);
}
