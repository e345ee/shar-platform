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

/**
 * "Activities" in REST layer map to existing Test/TestQuestion domain inside the codebase.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActivitiesController {

    private final TestService testService;

    // --- Create/list activities ---

    @PostMapping(value = "/lessons/{lessonId}/activities", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<TestDto> createLessonActivity(
            @PathVariable Integer lessonId,
            @Valid @RequestBody TestUpsertDto dto
    ) {
        TestDto created = testService.create(lessonId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/lessons/{lessonId}/activities")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<List<TestSummaryDto>> listByLesson(@PathVariable Integer lessonId) {
        return ResponseEntity.ok(testService.listByLesson(lessonId));
    }

    @PostMapping(value = "/courses/{courseId}/activities", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<TestDto> createCourseActivity(
            @PathVariable Integer courseId,
            @Valid @RequestBody CreateActivityDto dto
    ) {
        TestDto created = testService.createActivity(courseId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/courses/{courseId}/activities/weekly")
    @PreAuthorize("hasAnyRole('ADMIN','METHODIST','TEACHER','STUDENT')")
    public ResponseEntity<List<TestSummaryDto>> listWeeklyActivities(@PathVariable Integer courseId) {
        return ResponseEntity.ok(testService.listWeeklyActivitiesForCourse(courseId));
    }

    // --- CRUD for a single activity ---

    @GetMapping("/activities/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<?> getActivity(@PathVariable Integer id) {
        return ResponseEntity.ok(testService.getById(id));
    }

    @PutMapping(value = "/activities/{id}", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<TestDto> updateActivity(@PathVariable Integer id, @Valid @RequestBody TestUpsertDto dto) {
        return ResponseEntity.ok(testService.update(id, dto));
    }

    @DeleteMapping("/activities/{id}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<Void> deleteActivity(@PathVariable Integer id) {
        testService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/activities/{id}/publish")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<TestDto> publish(@PathVariable Integer id) {
        return ResponseEntity.ok(testService.markReady(id));
    }

    // --- Weekly scheduling ---

    @PostMapping(value = "/activities/{id}/schedule-week", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<TestDto> scheduleWeek(@PathVariable Integer id, @Valid @RequestBody AssignWeeklyActivityDto dto) {
        return ResponseEntity.ok(testService.assignWeeklyActivity(id, dto));
    }

    // --- Questions CRUD ---

    @PostMapping(value = "/activities/{activityId}/questions", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<TestQuestionDto> createQuestion(
            @PathVariable Integer activityId,
            @Valid @RequestBody TestQuestionUpsertDto dto
    ) {
        TestQuestionDto created = testService.createQuestion(activityId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping(value = "/activities/{activityId}/questions/{questionId}", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<TestQuestionDto> updateQuestion(
            @PathVariable Integer activityId,
            @PathVariable Integer questionId,
            @Valid @RequestBody TestQuestionUpsertDto dto
    ) {
        return ResponseEntity.ok(testService.updateQuestion(activityId, questionId, dto));
    }

    @DeleteMapping("/activities/{activityId}/questions/{questionId}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Integer activityId,
            @PathVariable Integer questionId
    ) {
        testService.deleteQuestion(activityId, questionId);
        return ResponseEntity.noContent().build();
    }
}
