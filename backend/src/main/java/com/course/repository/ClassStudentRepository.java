package com.course.repository;

import com.course.entity.ClassStudent;
import com.course.entity.StudyClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;

import java.util.Optional;

import com.course.entity.User;

public interface ClassStudentRepository extends JpaRepository<ClassStudent, Integer> {
    boolean existsByStudyClassIdAndStudentId(Integer classId, Integer studentId);

    boolean existsByStudyClassIdAndStudentIdAndCourseClosedAtIsNotNull(Integer classId, Integer studentId);

    @Query(value = "SELECT enroll_user_to_class(:userId, :classId)", nativeQuery = true)
    void enrollUserToClass(@Param("userId") Integer userId, @Param("classId") Integer classId);

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

    @Query("select cs.student from ClassStudent cs where cs.studyClass.id = :classId order by cs.student.name asc")
    List<User> findStudentsByClassId(@Param("classId") Integer classId, Pageable pageable);

    @Query("select count(cs) from ClassStudent cs where cs.studyClass.id = :classId")
    long countStudentsByClassId(@Param("classId") Integer classId);

    @Query("select distinct cs.studyClass from ClassStudent cs where cs.student.id = :studentId order by cs.studyClass.name asc")
    List<StudyClass> findDistinctStudyClassesByStudentId(@Param("studentId") Integer studentId);

    @Query("select cs.student.id from ClassStudent cs where cs.studyClass.id = :classId and cs.courseClosedAt is not null")
    List<Integer> findClosedStudentIdsByClassId(@Param("classId") Integer classId);
}


