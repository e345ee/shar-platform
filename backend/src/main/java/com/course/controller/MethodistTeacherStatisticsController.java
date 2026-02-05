package com.course.controller;

import com.course.dto.TeacherStatsDto;
import com.course.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SRS 3.1.3/3.1.4: teacher statistics and CSV export.
 */
@RestController
@RequiredArgsConstructor
public class MethodistTeacherStatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/api/methodist/statistics/teachers")
    @PreAuthorize("hasAnyRole('METHODIST','ADMIN')")
    public ResponseEntity<List<TeacherStatsDto>> getTeachersStats(
            @RequestParam(value = "methodistId", required = false) Integer methodistId
    ) {
        return ResponseEntity.ok(statisticsService.getTeacherStatsForCurrentMethodist(methodistId));
    }

    @GetMapping("/api/methodist/statistics/teachers/export/csv")
    @PreAuthorize("hasAnyRole('METHODIST','ADMIN')")
    public ResponseEntity<byte[]> exportTeachersStatsCsv(
            @RequestParam(value = "methodistId", required = false) Integer methodistId
    ) {
        String csv = statisticsService.exportTeacherStatsCsv(methodistId);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=teachers_statistics.csv");

        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
