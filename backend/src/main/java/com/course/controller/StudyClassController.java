package com.course.controller;

import com.course.dto.StudyClassDto;
import com.course.service.StudyClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StudyClassController {

    private final StudyClassService classService;

    @PostMapping("/api/classes")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<StudyClassDto> create(@Valid @RequestBody StudyClassDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classService.create(dto));
    }

    @GetMapping("/api/classes/{id}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<StudyClassDto> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(classService.getById(id));
    }

    @GetMapping("/api/courses/{courseId}/classes")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<List<StudyClassDto>> getByCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(classService.getAllByCourse(courseId));
    }

    @PutMapping("/api/classes/{id}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<StudyClassDto> update(@PathVariable Integer id, @Valid @RequestBody StudyClassDto dto) {
        return ResponseEntity.ok(classService.update(id, dto));
    }

    @DeleteMapping("/api/classes/{id}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        classService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
