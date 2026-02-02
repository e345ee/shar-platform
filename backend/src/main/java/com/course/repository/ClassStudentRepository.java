package com.course.repository;

import com.course.entity.ClassStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassStudentRepository extends JpaRepository<ClassStudent, Integer> {
    boolean existsByStudyClassIdAndStudentId(Integer classId, Integer studentId);

    @Query("select (count(cs) > 0) from ClassStudent cs where cs.student.id = :studentId and cs.studyClass.course.id = :courseId")
    boolean existsStudentInCourse(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    @Query("select (count(cs) > 0) from ClassStudent cs where cs.student.id = :studentId and cs.studyClass.teacher.id = :teacherId")
    boolean existsStudentInTeacherClasses(@Param("studentId") Integer studentId, @Param("teacherId") Integer teacherId);

    @Query("select (count(cs) > 0) from ClassStudent cs where cs.student.id = :studentId and cs.studyClass.course.id = :courseId and cs.studyClass.teacher.id = :teacherId")
    boolean existsStudentInTeacherCourse(@Param("studentId") Integer studentId, @Param("teacherId") Integer teacherId, @Param("courseId") Integer courseId);

    @Query("select (count(cs) > 0) from ClassStudent cs where cs.student.id = :studentId and cs.studyClass.course.createdBy.id = :methodistId")
    boolean existsStudentInMethodistCourses(@Param("studentId") Integer studentId, @Param("methodistId") Integer methodistId);
}

