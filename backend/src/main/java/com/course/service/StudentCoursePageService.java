package com.course.service;

import com.course.dto.*;
import com.course.entity.ActivityType;
import com.course.entity.Course;
import com.course.entity.Test;
import com.course.entity.TestStatus;
import com.course.entity.RoleName;
import com.course.entity.User;
import com.course.repository.LatestAttemptProjection;
import com.course.repository.TestAttemptRepository;
import com.course.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCoursePageService {

    private static final RoleName ROLE_STUDENT = RoleName.STUDENT;

    private final AuthService authService;
    private final UserService userService;

    private final CourseService courseService;
    private final LessonService lessonService;
    private final ClassOpenedLessonService classOpenedLessonService;
    private final ClassStudentService classStudentService;

    private final TestRepository testRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final TestService testService;

    public StudentCoursePageDto getCoursePage(Integer courseId) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_STUDENT);
        classStudentService.assertStudentInCourse(current.getId(), courseId, "Student is not enrolled in this course");

        Course course = courseService.getEntityById(courseId);

        // Opened lessons for this student inside the course
        List<LessonDto> allLessons = lessonService.listByCourse(courseId);
        List<Integer> openedIds = classOpenedLessonService.findOpenedLessonIdsForStudentInCourse(current.getId(), courseId);
        List<LessonDto> openedLessons = openedIds.isEmpty()
                ? List.of()
                : allLessons.stream().filter(l -> l.getId() != null && openedIds.contains(l.getId())).toList();

        // Lesson-bound activities (READY only) for opened lessons
        List<Test> lessonActivities = openedLessons.isEmpty()
                ? List.of()
                : testRepository.findAllByLesson_IdInAndStatusAndActivityTypeIn(
                        openedLessons.stream().map(LessonDto::getId).filter(Objects::nonNull).toList(),
                        TestStatus.READY,
                        List.of(ActivityType.HOMEWORK_TEST, ActivityType.CONTROL_WORK)
                );

        // Weekly activities for current week
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        List<Test> weekly = testRepository.findAllByCourse_IdAndActivityTypeAndStatusAndAssignedWeekStart(
                courseId, ActivityType.WEEKLY_STAR, TestStatus.READY, weekStart
        );

        // Collect all activity ids for latest attempt lookup
        Set<Integer> testIds = new LinkedHashSet<>();
        lessonActivities.forEach(t -> testIds.add(t.getId()));
        weekly.forEach(t -> testIds.add(t.getId()));

        Map<Integer, AttemptStatusDto> latestByTest = new HashMap<>();
        if (!testIds.isEmpty()) {
            List<LatestAttemptProjection> latest = testAttemptRepository.findLatestAttemptsForStudent(current.getId(), new ArrayList<>(testIds));
            for (LatestAttemptProjection p : latest) {
                AttemptStatusDto a = new AttemptStatusDto();
                a.setTestId(p.getTestId());
                a.setAttemptId(p.getAttemptId());
                a.setStatus(p.getStatus());
                a.setScore(p.getScore());
                a.setMaxScore(p.getMaxScore());
                a.setWeightedScore(p.getWeightedScore());
                a.setWeightedMaxScore(p.getWeightedMaxScore());
                a.setSubmittedAt(p.getSubmittedAt());
                latestByTest.put(p.getTestId(), a);
            }
        }

        // Build lesson groups
        Map<Integer, List<Test>> activitiesByLessonId = new HashMap<>();
        for (Test t : lessonActivities) {
            if (t.getLesson() != null && t.getLesson().getId() != null) {
                activitiesByLessonId.computeIfAbsent(t.getLesson().getId(), k -> new ArrayList<>()).add(t);
            }
        }

        List<LessonWithActivitiesDto> lessonBlocks = new ArrayList<>();
        for (LessonDto lesson : openedLessons) {
            LessonWithActivitiesDto block = new LessonWithActivitiesDto();
            block.setLesson(lesson);
            List<Test> acts = activitiesByLessonId.getOrDefault(lesson.getId(), List.of());
            block.setActivities(acts.stream().map(t -> toActivityWithAttempt(t, latestByTest.get(t.getId()))).toList());
            lessonBlocks.add(block);
        }

        StudentCoursePageDto dto = new StudentCoursePageDto();
        dto.setCourse(toCourseDto(course));
        dto.setLessons(lessonBlocks);
        dto.setWeeklyThisWeek(weekly.stream().map(t -> toActivityWithAttempt(t, latestByTest.get(t.getId()))).toList());
        return dto;
    }

    private ActivityWithAttemptDto toActivityWithAttempt(Test t, AttemptStatusDto attempt) {
        ActivityWithAttemptDto dto = new ActivityWithAttemptDto();
        // reuse existing mapper for summary
        TestSummaryDto summary = testService.toSummaryDto(t);
        dto.setActivity(summary);
        dto.setLatestAttempt(attempt);
        return dto;
    }

    private CourseDto toCourseDto(Course c) {
        CourseDto dto = new CourseDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setDescription(c.getDescription());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        if (c.getCreatedBy() != null) {
            dto.setCreatedById(c.getCreatedBy().getId());
            dto.setCreatedByName(c.getCreatedBy().getName());
        }
        return dto;
    }
}
