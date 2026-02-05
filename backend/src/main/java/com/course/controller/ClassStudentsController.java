package com.course.controller;

import com.course.service.ClassStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * SRS 3.2.1: Teacher removes a student from a study class.
 */
@RestController
@RequiredArgsConstructor
public class ClassStudentsController {

    private final ClassStudentService classStudentService;

    @DeleteMapping("/api/classes/{classId}/students/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<Void> removeStudentFromClass(@PathVariable Integer classId,
                                                       @PathVariable Integer studentId) {
        classStudentService.removeStudentFromClass(classId, studentId);
        return ResponseEntity.noContent().build();
    }
}
