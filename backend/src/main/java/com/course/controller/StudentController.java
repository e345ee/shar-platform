package com.course.controller;

import com.course.dto.CourseDto;
import com.course.dto.LessonDto;
import com.course.dto.StudentCoursePageDto;
import com.course.dto.TestAttemptDto;
import com.course.dto.TestSummaryDto;
import com.course.service.StudentContentService;
import com.course.service.StudentCoursePageService;
import com.course.service.TestAttemptService;
import com.course.service.TestService;
import com.course.service.CourseCompletionEmailService;
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
    private final StudentCoursePageService studentCoursePageService;
    private final TestAttemptService testAttemptService;
    private final TestService testService;
    private final CourseCompletionEmailService courseCompletionEmailService;

    
    @GetMapping("/courses/{courseId}/page")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentCoursePageDto> getCoursePage(@PathVariable Integer courseId) {
        return ResponseEntity.ok(studentCoursePageService.getCoursePage(courseId));
    }

    
    @GetMapping("/courses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<CourseDto>> listMyCourses() {
        return ResponseEntity.ok(studentContentService.listMyCourses());
    }

    
    @GetMapping("/courses/{courseId}/lessons")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<LessonDto>> listMyLessonsInCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(studentContentService.listMyLessonsInCourse(courseId));
    }

    
    @GetMapping("/tests/{testId}/attempts/latest")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TestAttemptDto> getLatestCompletedAttempt(@PathVariable Integer testId) {
        return ResponseEntity.ok(testAttemptService.getLatestCompletedAttemptForTest(testId));
    }

    
    @GetMapping("/lessons/{lessonId}/test/attempts/latest")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TestAttemptDto> getLatestCompletedAttemptByLesson(@PathVariable Integer lessonId) {
        List<TestSummaryDto> tests = testService.listByLesson(lessonId);
        if (tests == null || tests.isEmpty()) {
            
            throw new com.course.exception.ResourceNotFoundException("No test for lesson " + lessonId);
        }
        Integer testId = tests.get(0).getId();
        return ResponseEntity.ok(testAttemptService.getLatestCompletedAttemptForTest(testId));
    }

    
    @PostMapping("/courses/{courseId}/completion-email")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> sendCompletionEmail(@PathVariable Integer courseId) {
        courseCompletionEmailService.sendMyCompletionEmail(courseId);
        return ResponseEntity.ok().build();
    }
}
