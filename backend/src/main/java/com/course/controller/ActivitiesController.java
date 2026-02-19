package com.course.controller;

import com.course.dto.activity.ActivityCreateRequest;
import com.course.dto.activity.ActivityQuestionResponse;
import com.course.dto.activity.ActivityQuestionUpsertRequest;
import com.course.dto.activity.ActivityResponse;
import com.course.dto.activity.ActivityUpsertRequest;
import com.course.dto.activity.WeeklyActivityAssignRequest;
import com.course.service.TestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActivitiesController {

    private final TestService testService;


    @PostMapping(value = "/lessons/{lessonId}/activities", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<ActivityResponse> createLessonActivity(
            @PathVariable Integer lessonId,
            @Valid @RequestBody ActivityUpsertRequest dto
    ) {
        ActivityResponse created = testService.create(lessonId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/lessons/{lessonId}/activities")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<List<ActivityResponse>> listByLesson(@PathVariable Integer lessonId) {
        return ResponseEntity.ok(testService.listByLesson(lessonId));
    }

    @PostMapping(value = "/courses/{courseId}/activities", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<ActivityResponse> createCourseActivity(
            @PathVariable Integer courseId,
            @Valid @RequestBody ActivityCreateRequest dto
    ) {
        ActivityResponse created = testService.createActivity(courseId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/courses/{courseId}/activities/weekly")
    @PreAuthorize("hasAnyRole('ADMIN','METHODIST','TEACHER','STUDENT')")
    public ResponseEntity<List<ActivityResponse>> listWeeklyActivities(@PathVariable Integer courseId) {
        return ResponseEntity.ok(testService.listWeeklyActivitiesForCourse(courseId));
    }

    

    @GetMapping("/activities/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<?> getActivity(@PathVariable Integer id) {
        return ResponseEntity.ok(testService.getById(id));
    }

    @PutMapping(value = "/activities/{id}", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<ActivityResponse> updateActivity(@PathVariable Integer id, @Valid @RequestBody ActivityUpsertRequest dto) {
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
    public ResponseEntity<ActivityResponse> publish(@PathVariable Integer id) {
        return ResponseEntity.ok(testService.markReady(id));
    }

    

    @PostMapping(value = "/activities/{id}/schedule-week", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<ActivityResponse> scheduleWeek(@PathVariable Integer id, @Valid @RequestBody WeeklyActivityAssignRequest dto) {
        return ResponseEntity.ok(testService.assignWeeklyActivity(id, dto));
    }

    

    @PostMapping(value = "/activities/{activityId}/questions", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<ActivityQuestionResponse> createQuestion(
            @PathVariable Integer activityId,
            @Valid @RequestBody ActivityQuestionUpsertRequest dto
    ) {
        ActivityQuestionResponse created = testService.createQuestion(activityId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping(value = "/activities/{activityId}/questions/{questionId}", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<ActivityQuestionResponse> updateQuestion(
            @PathVariable Integer activityId,
            @PathVariable Integer questionId,
            @Valid @RequestBody ActivityQuestionUpsertRequest dto
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
