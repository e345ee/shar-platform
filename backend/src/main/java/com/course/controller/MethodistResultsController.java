package com.course.controller;

import com.course.dto.CourseTestAttemptSummaryDto;
import com.course.service.TestAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/methodist")
public class MethodistResultsController {

    private final TestAttemptService testAttemptService;

    /**
     * Methodist sees results (attempt summaries) for all tests in their course.
     */
    @GetMapping("/courses/{courseId}/test-attempts")
    @PreAuthorize("hasAnyRole('METHODIST','ADMIN')")
    public ResponseEntity<List<CourseTestAttemptSummaryDto>> listCourseAttempts(@PathVariable Integer courseId) {
        return ResponseEntity.ok(testAttemptService.listAttemptsForCourse(courseId));
    }
}
