package com.course.service;

import com.course.entity.*;
import com.course.repository.StudentRemedialAssignmentRepository;
import com.course.repository.TestAttemptRepository;
import com.course.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Assigns "задания для отстающих" (REMEDIAL_TASK) to students who show low results on a topic.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RemedialAssignmentService {

    /**
     * Threshold: if a student's result (percent) is strictly below this value, we consider the topic "weak".
     * Configurable via app.remedial.min-percent (default 50.0).
     */
    @Value("${app.remedial.min-percent:50.0}")
    private double minPercent;

    private final TestRepository testRepository;
    private final TestAttemptRepository attemptRepository;
    private final StudentRemedialAssignmentRepository assignmentRepository;

    /**
     * Called when an attempt becomes fully graded (autograded or after teacher grading).
     * If the attempt belongs to a lesson/control activity and the score is low, we assign a matching remedial task.
     */
    public void considerAssignAfterGrading(TestAttempt attempt) {
        if (attempt == null || attempt.getStatus() != TestAttemptStatus.GRADED) {
            return;
        }
        if (attempt.getTest() == null || attempt.getTest().getCourse() == null || attempt.getStudent() == null) {
            return;
        }

        Test test = attempt.getTest();
        if (test.getActivityType() != ActivityType.HOMEWORK_TEST && test.getActivityType() != ActivityType.CONTROL_WORK) {
            return;
        }

        Double percent = calcPercent(attempt.getScore(), attempt.getMaxScore());
        // User requirement: assign remedial when result is < 50% (strictly below threshold)
        if (percent == null || percent >= minPercent) {
            return;
        }

        Integer studentId = attempt.getStudent().getId();
        Integer courseId = test.getCourse().getId();
        String topic = test.getTopic();
        if (studentId == null || courseId == null || topic == null || topic.trim().isEmpty()) {
            return;
        }

        // If there is already an active remedial assignment for this topic, do nothing.
        if (assignmentRepository.existsByStudent_IdAndCourse_IdAndTopicAndCompletedAtIsNull(studentId, courseId, topic)) {
            return;
        }

        List<Test> candidates = testRepository
                .findAllByCourse_IdAndActivityTypeAndStatusAndTopicOrderByAssignedWeekStartDescIdDesc(
                        courseId, ActivityType.REMEDIAL_TASK, TestStatus.READY, topic
                );
        if (candidates.isEmpty()) {
            return;
        }

        // Do not assign activities from a future week: student must see the remedial task immediately.
        LocalDate currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        candidates = candidates.stream()
                .filter(t -> t.getAssignedWeekStart() == null || !t.getAssignedWeekStart().isAfter(currentWeekStart))
                .toList();
        if (candidates.isEmpty()) {
            return;
        }

        List<Integer> candidateIds = candidates.stream().map(Test::getId).filter(Objects::nonNull).toList();
        Set<Integer> completed = new HashSet<>(attemptRepository.findCompletedTestIdsForStudent(
                studentId,
                candidateIds,
                List.of(TestAttemptStatus.SUBMITTED, TestAttemptStatus.GRADED)
        ));

        for (Test cand : candidates) {
            if (cand == null || cand.getId() == null) {
                continue;
            }
            if (completed.contains(cand.getId())) {
                continue;
            }
            // idempotency: already assigned -> no further action
            if (assignmentRepository.existsByStudent_IdAndTest_Id(studentId, cand.getId())) {
                return;
            }

            StudentRemedialAssignment a = new StudentRemedialAssignment();
            a.setStudent(attempt.getStudent());
            a.setCourse(test.getCourse());
            a.setTest(cand);
            a.setTopic(topic);
            a.setAssignedWeekStart(cand.getAssignedWeekStart());
            a.setAssignedAt(LocalDateTime.now());
            a.setCompletedAt(null);

            assignmentRepository.save(a);
            return;
        }
    }

    /**
     * Marks an existing remedial assignment as completed when the student finishes the remedial activity.
     */
    public void markCompletedIfRemedial(TestAttempt attempt) {
        if (attempt == null || attempt.getTest() == null || attempt.getStudent() == null) {
            return;
        }
        if (attempt.getTest().getActivityType() != ActivityType.REMEDIAL_TASK) {
            return;
        }
        if (attempt.getStatus() != TestAttemptStatus.SUBMITTED && attempt.getStatus() != TestAttemptStatus.GRADED) {
            return;
        }

        Integer studentId = attempt.getStudent().getId();
        Integer testId = attempt.getTest().getId();
        if (studentId == null || testId == null) {
            return;
        }

        assignmentRepository.findFirstByStudent_IdAndTest_Id(studentId, testId).ifPresent(a -> {
            if (a.getCompletedAt() == null) {
                a.setCompletedAt(LocalDateTime.now());
                assignmentRepository.save(a);
            }
        });
    }

    private Double calcPercent(Integer score, Integer max) {
        if (score == null || max == null || max <= 0) {
            return null;
        }
        return (score * 100.0) / max;
    }
}
