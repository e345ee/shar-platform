package com.course.controller;

import com.course.dto.course.CourseResponse;
import com.course.dto.course.CourseUpsertRequest;
import com.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CoursesController {

    private final CourseService courseService;

    @PostMapping
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseUpsertRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<CourseResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(courseService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<List<CourseResponse>> getAll() {
        return ResponseEntity.ok(courseService.getAll());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<CourseResponse> update(@PathVariable Integer id, @Valid @RequestBody CourseUpsertRequest dto) {
        return ResponseEntity.ok(courseService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
