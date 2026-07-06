package com.course.repository;

import com.course.entity.StudentRemedialAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentRemedialAssignmentRepository extends JpaRepository<StudentRemedialAssignment, Integer> {

    boolean existsByStudent_IdAndTest_Id(Integer studentId, Integer testId);

    Optional<StudentRemedialAssignment> findFirstByStudent_IdAndTest_Id(Integer studentId, Integer testId);

    List<StudentRemedialAssignment> findAllByStudent_IdAndCourse_IdAndAssignedWeekStart(Integer studentId, Integer courseId, LocalDate assignedWeekStart);

    List<StudentRemedialAssignment> findAllByStudent_IdAndCourse_Id(Integer studentId, Integer courseId);

    boolean existsByStudent_IdAndCourse_IdAndTopicAndCompletedAtIsNull(Integer studentId, Integer courseId, String topic);
}
