package com.course.controller;

import com.course.dto.LessonPresentationInfoDto;
import com.course.service.LessonPresentationSlideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LessonPresentationController {

    private final LessonPresentationSlideService slideService;

    /**
     * Returns page count for the lesson presentation (used by frontend to know max slide number).
     */
    @GetMapping("/api/lessons/{id}/presentation/info")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<LessonPresentationInfoDto> getPresentationInfo(@PathVariable Integer id) {
        return ResponseEntity.ok(slideService.getPresentationInfo(id));
    }

    /**
     * Returns a single PDF page rendered as PNG.
     * Page numbering is 1-based.
     */
    @GetMapping(value = "/api/lessons/{id}/presentation/pages/{page}", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<byte[]> getPresentationPage(
            @PathVariable Integer id,
            @PathVariable int page,
            @RequestParam(name = "dpi", required = false) Integer dpi
    ) {
        return slideService.renderPageAsPng(id, page, dpi);
    }
}
