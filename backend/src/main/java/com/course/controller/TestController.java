package com.course.controller;

import com.course.dto.*;
import com.course.service.TestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    

    @PostMapping(value = "/api/lessons/{lessonId}/tests", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<TestDto> createTest(
            @PathVariable Integer lessonId,
            @Valid @RequestBody TestUpsertDto dto
    ) {
        TestDto created = testService.create(lessonId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/lessons/{lessonId}/tests")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<List<TestSummaryDto>> listByLesson(@PathVariable Integer lessonId) {
        return ResponseEntity.ok(testService.listByLesson(lessonId));
    }

    @GetMapping("/api/tests/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<?> getTest(@PathVariable Integer id) {
        return ResponseEntity.ok(testService.getById(id));
    }

    @PutMapping(value = "/api/tests/{id}", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<TestDto> updateTest(
            @PathVariable Integer id,
            @Valid @RequestBody TestUpsertDto dto
    ) {
        return ResponseEntity.ok(testService.update(id, dto));
    }

    @DeleteMapping("/api/tests/{id}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<Void> deleteTest(@PathVariable Integer id) {
        testService.delete(id);
        return ResponseEntity.noContent().build();
    }

    
    @PostMapping("/api/tests/{id}/ready")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<TestDto> markReady(@PathVariable Integer id) {
        return ResponseEntity.ok(testService.markReady(id));
    }

    

    @PostMapping(value = "/api/tests/{testId}/questions", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<TestQuestionDto> createQuestion(
            @PathVariable Integer testId,
            @Valid @RequestBody TestQuestionUpsertDto dto
    ) {
        TestQuestionDto created = testService.createQuestion(testId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping(value = "/api/tests/{testId}/questions/{questionId}", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<TestQuestionDto> updateQuestion(
            @PathVariable Integer testId,
            @PathVariable Integer questionId,
            @Valid @RequestBody TestQuestionUpsertDto dto
    ) {
        return ResponseEntity.ok(testService.updateQuestion(testId, questionId, dto));
    }

    @DeleteMapping("/api/tests/{testId}/questions/{questionId}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Integer testId,
            @PathVariable Integer questionId
    ) {
        testService.deleteQuestion(testId, questionId);
        return ResponseEntity.noContent().build();
    }
}
