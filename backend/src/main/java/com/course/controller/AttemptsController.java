package com.course.controller;

import com.course.dto.*;
import com.course.service.TestAttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AttemptsController {

    private final TestAttemptService attemptService;

    @PostMapping("/activities/{activityId}/attempts")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TestAttemptDto> startAttempt(@PathVariable Integer activityId) {
        var result = attemptService.startAttempt(activityId);
        return ResponseEntity.status(result.created() ? 201 : 200).body(result.attempt());
    }

    @GetMapping("/activities/{activityId}/attempts/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<TestAttemptSummaryDto>> listMyAttempts(@PathVariable Integer activityId) {
        return ResponseEntity.ok(attemptService.listMyAttempts(activityId));
    }

    @GetMapping("/activities/{activityId}/attempts")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST')")
    public ResponseEntity<List<TestAttemptSummaryDto>> listAllAttempts(@PathVariable Integer activityId) {
        return ResponseEntity.ok(attemptService.listAllAttempts(activityId));
    }

    @GetMapping("/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<TestAttemptDto> getAttempt(@PathVariable Integer attemptId) {
        return ResponseEntity.ok(attemptService.getAttempt(attemptId));
    }

    @PostMapping(value = "/attempts/{attemptId}/submit", consumes = {"application/json"})
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TestAttemptDto> submit(
            @PathVariable Integer attemptId,
            @Valid @RequestBody SubmitTestAttemptDto dto
    ) {
        return ResponseEntity.ok(attemptService.submit(attemptId, dto));
    }

    @PutMapping(value = "/attempts/{attemptId}/grade", consumes = {"application/json"})
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST')")
    public ResponseEntity<TestAttemptDto> gradeOpenAttempt(
            @PathVariable Integer attemptId,
            @Valid @RequestBody GradeTestAttemptDto dto
    ) {
        return ResponseEntity.ok(attemptService.gradeOpenAttempt(attemptId, dto));
    }

    @GetMapping("/attempts/pending")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<List<PendingTestAttemptDto>> listPendingAttempts(
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) Integer activityId,
            @RequestParam(required = false) Integer classId
    ) {
        // service param name is testId, but REST uses activityId
        return ResponseEntity.ok(attemptService.listPendingAttemptsForTeacher(courseId, activityId, classId));
    }

    // --- Methodist statistics-style view: all attempts inside a course ---

    @GetMapping("/courses/{courseId}/attempts")
    @PreAuthorize("hasAnyRole('METHODIST','ADMIN')")
    public ResponseEntity<List<CourseTestAttemptSummaryDto>> listCourseAttempts(@PathVariable Integer courseId) {
        return ResponseEntity.ok(attemptService.listAttemptsForCourse(courseId));
    }
}
