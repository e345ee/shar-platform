package com.course.repository;

import com.course.entity.MethodistTeacher;
import com.course.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface MethodistTeacherRepository extends JpaRepository<MethodistTeacher, Integer> {

    boolean existsByMethodist_IdAndTeacher_Id(Integer methodistId, Integer teacherId);

    long countByTeacher_Id(Integer teacherId);

    @Modifying
    @Query("delete from MethodistTeacher mt where mt.methodist.id = :methodistId and mt.teacher.id = :teacherId")
    int deleteLink(@Param("methodistId") Integer methodistId, @Param("teacherId") Integer teacherId);

    @Query("select mt.teacher from MethodistTeacher mt where mt.methodist.id = :methodistId and mt.teacher.deleted = false order by mt.teacher.name asc")
    Page<User> findTeachersByMethodistId(@Param("methodistId") Integer methodistId, Pageable pageable);
}
