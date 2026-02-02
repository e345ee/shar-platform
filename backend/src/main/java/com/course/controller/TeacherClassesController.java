package com.course.controller;

import com.course.dto.StudyClassDto;
import com.course.service.StudyClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
public class TeacherClassesController {

    private final StudyClassService classService;

    @GetMapping("/api/teachers/me/classes")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<List<StudyClassDto>> getMyClasses() {
        return ResponseEntity.ok(classService.getMyClasses());
    }

    @GetMapping("/api/teachers/me/classes/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<StudyClassDto> getMyClassById(@PathVariable Integer id) {
        return ResponseEntity.ok(classService.getMyClassById(id));
    }
}
