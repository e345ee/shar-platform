package com.course.controller;

import com.course.dto.*;
import com.course.service.TestAttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TestAttemptController {

    private final TestAttemptService attemptService;

    /**
     * Student starts an attempt for a READY test.
     */
    @PostMapping("/api/tests/{testId}/attempts/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TestAttemptDto> startAttempt(@PathVariable Integer testId) {
        TestAttemptDto created = attemptService.startAttempt(testId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Student lists own attempts for a test.
     */
    @GetMapping("/api/tests/{testId}/attempts/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<TestAttemptSummaryDto>> listMyAttempts(@PathVariable Integer testId) {
        return ResponseEntity.ok(attemptService.listMyAttempts(testId));
    }

    /**
     * Teacher/methodist/admin lists all attempts for a test.
     */
    @GetMapping("/api/tests/{testId}/attempts")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST')")
    public ResponseEntity<List<TestAttemptSummaryDto>> listAllAttempts(@PathVariable Integer testId) {
        return ResponseEntity.ok(attemptService.listAllAttempts(testId));
    }

    /**
     * Get attempt details.
     */
    @GetMapping("/api/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<TestAttemptDto> getAttempt(@PathVariable Integer attemptId) {
        return ResponseEntity.ok(attemptService.getAttempt(attemptId));
    }

    /**
     * Student submits attempt answers. Autograde is performed immediately.
     */
    @PostMapping(value = "/api/attempts/{attemptId}/submit", consumes = {"application/json"})
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TestAttemptDto> submit(
            @PathVariable Integer attemptId,
            @Valid @RequestBody SubmitTestAttemptDto dto
    ) {
        return ResponseEntity.ok(attemptService.submit(attemptId, dto));
    }
}
