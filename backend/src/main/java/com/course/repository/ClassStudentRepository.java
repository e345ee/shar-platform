package com.course.repository;

import com.course.entity.ClassStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClassStudentRepository extends JpaRepository<ClassStudent, Integer> {
    boolean existsByStudyClassIdAndStudentId(Integer classId, Integer studentId);

    Optional<ClassStudent> findByStudyClassIdAndStudentId(Integer classId, Integer studentId);

    @Query("select cs.studyClass.id from ClassStudent cs where cs.student.id = :studentId and cs.studyClass.course.id = :courseId")
    java.util.List<Integer> findClassIdsByStudentInCourse(@Param("studentId") Integer studentId,
                                                         @Param("courseId") Integer courseId);

    @Query("select (count(cs) > 0) from ClassStudent cs where cs.student.id = :studentId and cs.studyClass.course.id = :courseId")
    boolean existsStudentInCourse(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    @Query("select (count(cs) > 0) from ClassStudent cs " +
            "where cs.student.id = :studentId and cs.studyClass.course.id = :courseId and cs.courseClosedAt is not null")
    boolean existsClosedCourseForStudent(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    @Query("select (count(cs) > 0) from ClassStudent cs where cs.student.id = :studentId and cs.studyClass.teacher.id = :teacherId")
    boolean existsStudentInTeacherClasses(@Param("studentId") Integer studentId, @Param("teacherId") Integer teacherId);

    @Query("select (count(cs) > 0) from ClassStudent cs where cs.student.id = :studentId and cs.studyClass.course.id = :courseId and cs.studyClass.teacher.id = :teacherId")
    boolean existsStudentInTeacherCourse(@Param("studentId") Integer studentId, @Param("teacherId") Integer teacherId, @Param("courseId") Integer courseId);

    @Query("select (count(cs) > 0) from ClassStudent cs where cs.student.id = :studentId and cs.studyClass.course.createdBy.id = :methodistId")
    boolean existsStudentInMethodistCourses(@Param("studentId") Integer studentId, @Param("methodistId") Integer methodistId);


@Query("select distinct c from ClassStudent cs join cs.studyClass sc join sc.course c " +
        "where cs.student.id = :studentId order by c.name asc")
java.util.List<com.course.entity.Course> findDistinctCoursesByStudentId(@Param("studentId") Integer studentId);

    @Query("select distinct cs.student.id from ClassStudent cs where cs.studyClass.course.id = :courseId")
    java.util.List<Integer> findDistinctStudentIdsByCourseId(@Param("courseId") Integer courseId);

    @Query("select distinct cs.studyClass.teacher.id from ClassStudent cs where cs.student.id = :studentId and cs.studyClass.course.id = :courseId and cs.studyClass.teacher.id is not null")
    java.util.List<Integer> findDistinctTeacherIdsByStudentInCourse(@Param("studentId") Integer studentId,
                                                                    @Param("courseId") Integer courseId);

}


