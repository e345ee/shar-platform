package com.course.controller;

import com.course.dto.CourseDto;
import com.course.dto.LessonDto;
import com.course.dto.TestAttemptDto;
import com.course.dto.TestSummaryDto;
import com.course.service.StudentContentService;
import com.course.service.TestAttemptService;
import com.course.service.TestService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/student")
public class StudentController {

    private final StudentContentService studentContentService;
    private final TestAttemptService testAttemptService;
    private final TestService testService;

    /**
     * Student gets all courses where they are enrolled (via any class in the course).
     */
    @GetMapping("/courses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<CourseDto>> listMyCourses() {
        return ResponseEntity.ok(studentContentService.listMyCourses());
    }

    /**
     * Student gets all lessons inside a course they belong to.
     */
    @GetMapping("/courses/{courseId}/lessons")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<LessonDto>> listMyLessonsInCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(studentContentService.listMyLessonsInCourse(courseId));
    }

    /**
     * Student gets latest submitted/graded attempt details for a test.
     */
    @GetMapping("/tests/{testId}/attempts/latest")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TestAttemptDto> getLatestCompletedAttempt(@PathVariable Integer testId) {
        return ResponseEntity.ok(testAttemptService.getLatestCompletedAttemptForTest(testId));
    }

    /**
     * Convenience endpoint: by lessonId (test is attached to a lesson).
     * If lesson has no tests -> 404.
     */
    @GetMapping("/lessons/{lessonId}/test/attempts/latest")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TestAttemptDto> getLatestCompletedAttemptByLesson(@PathVariable Integer lessonId) {
        List<TestSummaryDto> tests = testService.listByLesson(lessonId);
        if (tests == null || tests.isEmpty()) {
            // TestService throws Lesson access checks; here we just align behavior
            throw new com.course.exception.ResourceNotFoundException("No test for lesson " + lessonId);
        }
        Integer testId = tests.get(0).getId();
        return ResponseEntity.ok(testAttemptService.getLatestCompletedAttemptForTest(testId));
    }
}
