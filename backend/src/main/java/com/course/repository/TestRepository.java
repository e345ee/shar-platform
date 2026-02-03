package com.course.repository;

import com.course.entity.ActivityType;
import com.course.entity.Test;
import com.course.entity.TestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TestRepository extends JpaRepository<Test, Integer> {

    boolean existsByLesson_IdAndActivityType(Integer lessonId, ActivityType activityType);

    Optional<Test> findByLesson_IdAndActivityType(Integer lessonId, ActivityType activityType);

    List<Test> findAllByLesson_Id(Integer lessonId);

    List<Test> findAllByLesson_IdAndStatus(Integer lessonId, TestStatus status);

    List<Test> findAllByCourse_IdAndActivityTypeAndStatusAndAssignedWeekStartNotNullOrderByAssignedWeekStartDesc(
            Integer courseId, ActivityType activityType, TestStatus status
    );

    Optional<Test> findByIdAndCourse_Id(Integer id, Integer courseId);

    List<Test> findAllByCourse_IdAndActivityTypeAndStatusAndAssignedWeekStart(
            Integer courseId, ActivityType activityType, TestStatus status, LocalDate assignedWeekStart
    );

    List<Test> findAllByLesson_IdInAndStatusAndActivityTypeIn(
            Collection<Integer> lessonIds,
            TestStatus status,
            Collection<ActivityType> activityTypes
    );
}
