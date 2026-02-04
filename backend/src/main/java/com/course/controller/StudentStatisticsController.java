package com.course.controller;

import com.course.dto.StudentStatisticsOverviewDto;
import com.course.dto.StudentTopicStatsDto;
import com.course.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/student/statistics")
public class StudentStatisticsController {

    private final StatisticsService statisticsService;

    /**
     * Overview: attempts/tests counts + course completion.
     */
    @GetMapping("/overview")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentStatisticsOverviewDto> overview() {
        return ResponseEntity.ok(statisticsService.getMyOverview());
    }

    /**
     * Aggregated statistics by topic (theme).
     * Optional courseId filters to a single course.
     */
    @GetMapping("/topics")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentTopicStatsDto>> topics(@RequestParam(required = false) Integer courseId) {
        return ResponseEntity.ok(statisticsService.getMyTopicStats(courseId));
    }
}
