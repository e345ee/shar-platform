package com.course.controller;

import com.course.dto.*;
import com.course.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    // --- Teacher / Methodist (topics stats) ---

    @GetMapping("/classes/{classId}/topics")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<List<ClassTopicStatsDto>> classTopics(@PathVariable Integer classId) {
        return ResponseEntity.ok(statisticsService.getClassTopicStatsForTeacher(classId));
    }

    @GetMapping("/students/{studentId}/topics")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<List<StudentTopicStatsDto>> studentTopics(
            @PathVariable Integer studentId,
            @RequestParam Integer courseId
    ) {
        return ResponseEntity.ok(statisticsService.getStudentTopicStatsForTeacher(studentId, courseId));
    }

    // --- Methodist / Admin (course-wide stats) ---

    @GetMapping("/courses/{courseId}/topics")
    @PreAuthorize("hasAnyRole('METHODIST','ADMIN')")
    public ResponseEntity<List<CourseTopicStatsDto>> courseTopics(@PathVariable Integer courseId) {
        return ResponseEntity.ok(statisticsService.getCourseTopicStatsForMethodist(courseId));
    }

    @GetMapping("/courses/{courseId}/classes/topics")
    @PreAuthorize("hasAnyRole('METHODIST','ADMIN')")
    public ResponseEntity<List<TeacherClassTopicStatsDto>> classTopicsForCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(statisticsService.getCourseTeacherTopicStatsForMethodist(courseId));
    }

    // --- Teachers performance (methodist/admin) ---

    @GetMapping("/teachers")
    @PreAuthorize("hasAnyRole('METHODIST','ADMIN')")
    public ResponseEntity<List<TeacherStatsDto>> getTeachersStats(
            @RequestParam(value = "methodistId", required = false) Integer methodistId
    ) {
        return ResponseEntity.ok(statisticsService.getTeacherStatsForCurrentMethodist(methodistId));
    }

    @GetMapping("/teachers/export/csv")
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
