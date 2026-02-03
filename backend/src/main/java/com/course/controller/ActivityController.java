package com.course.controller;

import com.course.dto.AssignWeeklyActivityDto;
import com.course.dto.CreateActivityDto;
import com.course.dto.TestDto;
import com.course.dto.TestSummaryDto;
import com.course.service.TestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Generic activities API (beyond lesson tests).
 *
 * Note: Internally activities reuse the Test domain model (questions/attempts),
 * but are differentiated by activityType.
 */
@RestController
@RequiredArgsConstructor
public class ActivityController {

    private final TestService testService;

    @PostMapping(value = "/api/courses/{courseId}/activities", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<TestDto> createActivity(
            @PathVariable Integer courseId,
            @Valid @RequestBody CreateActivityDto dto
    ) {
        TestDto created = testService.createActivity(courseId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Assign WEEKLY_STAR to a specific week (Monday date). Activity must be READY.
     */
    @PostMapping(value = "/api/activities/{id}/assign-week", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<TestDto> assignWeek(
            @PathVariable Integer id,
            @Valid @RequestBody AssignWeeklyActivityDto dto
    ) {
        return ResponseEntity.ok(testService.assignWeeklyActivity(id, dto));
    }

    /**
     * Weekly "star" activities visible on course page.
     * For students: only READY and assigned.
     * For methodists/admins: currently returns READY+assigned (drafts are not shown on course page).
     */
    @GetMapping("/api/courses/{courseId}/weekly-activities")
    @PreAuthorize("hasAnyRole('ADMIN','METHODIST','TEACHER','STUDENT')")
    public ResponseEntity<List<TestSummaryDto>> listWeeklyActivities(@PathVariable Integer courseId) {
        return ResponseEntity.ok(testService.listWeeklyActivitiesForCourse(courseId));
    }
}
