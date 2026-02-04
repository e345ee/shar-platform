package com.course.controller;

import com.course.dto.CourseTopicStatsDto;
import com.course.dto.TeacherClassTopicStatsDto;
import com.course.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/methodist/statistics")
public class MethodistStatisticsController {

    private final StatisticsService statisticsService;

    /**
     * Course-level topic statistics (aggregated across all enrolled students).
     */
    @GetMapping("/courses/{courseId}/topics")
    @PreAuthorize("hasAnyRole('METHODIST','ADMIN')")
    public ResponseEntity<List<CourseTopicStatsDto>> courseTopics(@PathVariable Integer courseId) {
        return ResponseEntity.ok(statisticsService.getCourseTopicStatsForMethodist(courseId));
    }

    /**
     * Topic statistics split by classes (and their teachers) inside a course.
     * Useful to compare how different teachers/classes perform.
     */
    @GetMapping("/courses/{courseId}/classes/topics")
    @PreAuthorize("hasAnyRole('METHODIST','ADMIN')")
    public ResponseEntity<List<TeacherClassTopicStatsDto>> classTopics(@PathVariable Integer courseId) {
        return ResponseEntity.ok(statisticsService.getCourseTeacherTopicStatsForMethodist(courseId));
    }
}
