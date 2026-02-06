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
@RequiredArgsConstructor
public class TestAttemptController {

    private final TestAttemptService attemptService;

    
    @PostMapping("/api/tests/{testId}/attempts/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TestAttemptDto> startAttempt(@PathVariable Integer testId) {
        var result = attemptService.startAttempt(testId);
        return ResponseEntity.status(result.created() ? 201 : 200).body(result.attempt());
    }

    
    @GetMapping("/api/tests/{testId}/attempts/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<TestAttemptSummaryDto>> listMyAttempts(@PathVariable Integer testId) {
        return ResponseEntity.ok(attemptService.listMyAttempts(testId));
    }

    
    @GetMapping("/api/tests/{testId}/attempts")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST')")
    public ResponseEntity<List<TestAttemptSummaryDto>> listAllAttempts(@PathVariable Integer testId) {
        return ResponseEntity.ok(attemptService.listAllAttempts(testId));
    }

    
    @GetMapping("/api/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<TestAttemptDto> getAttempt(@PathVariable Integer attemptId) {
        return ResponseEntity.ok(attemptService.getAttempt(attemptId));
    }

    
    @PostMapping(value = "/api/attempts/{attemptId}/submit", consumes = {"application/json"})
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TestAttemptDto> submit(
            @PathVariable Integer attemptId,
            @Valid @RequestBody SubmitTestAttemptDto dto
    ) {
        return ResponseEntity.ok(attemptService.submit(attemptId, dto));
    }

    
    @PutMapping(value = "/api/attempts/{attemptId}/grade", consumes = {"application/json"})
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST')")
    public ResponseEntity<TestAttemptDto> gradeOpenAttempt(
            @PathVariable Integer attemptId,
            @Valid @RequestBody GradeTestAttemptDto dto
    ) {
        return ResponseEntity.ok(attemptService.gradeOpenAttempt(attemptId, dto));
    }

    
    @GetMapping("/api/teachers/me/attempts/pending")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<List<PendingTestAttemptDto>> listPendingAttempts(
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) Integer testId,
            @RequestParam(required = false) Integer classId
    ) {
        return ResponseEntity.ok(attemptService.listPendingAttemptsForTeacher(courseId, testId, classId));
    }
}
