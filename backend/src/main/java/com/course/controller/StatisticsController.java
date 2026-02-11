package com.course.controller;

import com.course.dto.statistics.StudentTopicStatsResponse;
import com.course.dto.statistics.TeacherStatsResponse;
import com.course.dto.statistics.TopicStatsResponse;
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

    

    @GetMapping("/classes/{classId}/topics")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<List<TopicStatsResponse>> classTopics(@PathVariable Integer classId) {
        return ResponseEntity.ok(statisticsService.getClassTopicStatsForTeacher(classId));
    }

    @GetMapping("/students/{studentId}/topics")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<List<StudentTopicStatsResponse>> studentTopics(
            @PathVariable Integer studentId,
            @RequestParam Integer courseId
    ) {
        return ResponseEntity.ok(statisticsService.getStudentTopicStatsForTeacher(studentId, courseId));
    }

    

    @GetMapping("/courses/{courseId}/topics")
    @PreAuthorize("hasAnyRole('METHODIST','ADMIN')")
    public ResponseEntity<List<TopicStatsResponse>> courseTopics(@PathVariable Integer courseId) {
        return ResponseEntity.ok(statisticsService.getCourseTopicStatsForMethodist(courseId));
    }

    @GetMapping("/courses/{courseId}/classes/topics")
    @PreAuthorize("hasAnyRole('METHODIST','ADMIN')")
    public ResponseEntity<List<TopicStatsResponse>> classTopicsForCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(statisticsService.getCourseTeacherTopicStatsForMethodist(courseId));
    }

    

    @GetMapping("/teachers")
    @PreAuthorize("hasAnyRole('METHODIST','ADMIN')")
    public ResponseEntity<List<TeacherStatsResponse>> getTeachersStats(
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
