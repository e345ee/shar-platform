package com.course.controller;

import com.course.dto.ClassTopicStatsDto;
import com.course.dto.StudentTopicStatsDto;
import com.course.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teachers/me/statistics")
public class TeacherStatisticsController {

    private final StatisticsService statisticsService;

    /**
     * Teacher sees statistics by topics for a specific class.
     */
    @GetMapping("/classes/{classId}/topics")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<List<ClassTopicStatsDto>> classTopics(@PathVariable Integer classId) {
        return ResponseEntity.ok(statisticsService.getClassTopicStatsForTeacher(classId));
    }

    /**
     * Teacher sees statistics by topics for a concrete student inside a course.
     */
    @GetMapping("/students/{studentId}/topics")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<List<StudentTopicStatsDto>> studentTopics(
            @PathVariable Integer studentId,
            @RequestParam Integer courseId
    ) {
        return ResponseEntity.ok(statisticsService.getStudentTopicStatsForTeacher(studentId, courseId));
    }
}
