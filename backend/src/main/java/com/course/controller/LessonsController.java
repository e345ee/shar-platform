package com.course.controller;

import com.course.dto.lesson.LessonPresentationInfoResponse;
import com.course.dto.lesson.LessonResponse;
import com.course.dto.lesson.LessonUpdateRequest;
import com.course.dto.lesson.LessonUpsertForm;
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

    

    @PostMapping(value = "/courses/{courseId}/lessons", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<LessonResponse> create(
            @PathVariable Integer courseId,
            @Valid @ModelAttribute LessonUpsertForm form
    ) {
        LessonResponse created = lessonService.create(courseId, form);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/courses/{courseId}/lessons")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<List<LessonResponse>> listByCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(lessonService.listByCourse(courseId));
    }

    @GetMapping("/lessons/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<LessonResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(lessonService.getById(id));
    }

    @PutMapping(value = "/lessons/{id}", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<LessonResponse> update(@PathVariable Integer id, @Valid @RequestBody LessonUpdateRequest dto) {
        return ResponseEntity.ok(lessonService.update(id, dto));
    }

    @PutMapping(value = "/lessons/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<LessonResponse> updateWithOptionalPresentation(
            @PathVariable Integer id,
            @Valid @ModelAttribute LessonUpsertForm form
    ) {
        return ResponseEntity.ok(lessonService.updateWithOptionalPresentation(id, form));
    }

    @PutMapping(value = "/lessons/{id}/presentation", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<LessonResponse> replacePresentation(
            @PathVariable Integer id,
            @RequestPart("presentation") MultipartFile presentation
    ) {
        return ResponseEntity.ok(lessonService.replacePresentation(id, presentation));
    }

    @DeleteMapping("/lessons/{id}/presentation")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<LessonResponse> deletePresentation(@PathVariable Integer id) {
        return ResponseEntity.ok(lessonService.deletePresentation(id));
    }

    @DeleteMapping("/lessons/{id}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        lessonService.delete(id);
        return ResponseEntity.noContent().build();
    }

    

    @GetMapping("/lessons/{id}/presentation/info")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<LessonPresentationInfoResponse> getPresentationInfo(@PathVariable Integer id) {
        return ResponseEntity.ok(slideService.getPresentationInfo(id));
    }


    @GetMapping("/lessons/{id}/open-classes")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST')")
    public ResponseEntity<List<Integer>> listOpenClassesForLesson(@PathVariable Integer id) {
        return ResponseEntity.ok(lessonService.listOpenClassIdsForLesson(id));
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
