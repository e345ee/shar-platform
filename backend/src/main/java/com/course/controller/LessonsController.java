package com.course.controller;

import com.course.dto.LessonDto;
import com.course.dto.LessonForm;
import com.course.dto.LessonPresentationInfoDto;
import com.course.dto.UpdateLessonDto;
import com.course.service.LessonPresentationSlideService;
import com.course.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LessonsController {

    private final LessonService lessonService;
    private final LessonPresentationSlideService slideService;

    // --- Lessons ---

    @PostMapping(value = "/courses/{courseId}/lessons", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<LessonDto> create(
            @PathVariable Integer courseId,
            @Valid @ModelAttribute LessonForm form
    ) {
        LessonDto created = lessonService.create(courseId, form);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/courses/{courseId}/lessons")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<List<LessonDto>> listByCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(lessonService.listByCourse(courseId));
    }

    @GetMapping("/lessons/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<LessonDto> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(lessonService.getById(id));
    }

    @PutMapping(value = "/lessons/{id}", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<LessonDto> update(@PathVariable Integer id, @Valid @RequestBody UpdateLessonDto dto) {
        return ResponseEntity.ok(lessonService.update(id, dto));
    }

    @PutMapping(value = "/lessons/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<LessonDto> updateWithOptionalPresentation(
            @PathVariable Integer id,
            @Valid @ModelAttribute LessonForm form
    ) {
        return ResponseEntity.ok(lessonService.updateWithOptionalPresentation(id, form));
    }

    @PutMapping(value = "/lessons/{id}/presentation", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<LessonDto> replacePresentation(
            @PathVariable Integer id,
            @RequestPart("presentation") MultipartFile presentation
    ) {
        return ResponseEntity.ok(lessonService.replacePresentation(id, presentation));
    }

    @DeleteMapping("/lessons/{id}/presentation")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<LessonDto> deletePresentation(@PathVariable Integer id) {
        return ResponseEntity.ok(lessonService.deletePresentation(id));
    }

    @DeleteMapping("/lessons/{id}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        lessonService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Presentation rendering ---

    @GetMapping("/lessons/{id}/presentation/info")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<LessonPresentationInfoDto> getPresentationInfo(@PathVariable Integer id) {
        return ResponseEntity.ok(slideService.getPresentationInfo(id));
    }

    @GetMapping(value = "/lessons/{id}/presentation/pages/{page}", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<byte[]> getPresentationPage(
            @PathVariable Integer id,
            @PathVariable int page,
            @RequestParam(name = "dpi", required = false) Integer dpi
    ) {
        return slideService.renderPageAsPng(id, page, dpi);
    }
}
