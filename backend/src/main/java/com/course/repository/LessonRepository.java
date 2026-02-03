package com.course.repository;

import com.course.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Integer> {

    List<Lesson> findAllByCourse_IdOrderByOrderIndexAsc(Integer courseId);

    boolean existsByCourse_IdAndTitleIgnoreCase(Integer courseId, String title);

    @Query("select coalesce(max(l.orderIndex), 0) from Lesson l where l.course.id = :courseId")
    int findMaxOrderIndexInCourse(@Param("courseId") Integer courseId);
}
