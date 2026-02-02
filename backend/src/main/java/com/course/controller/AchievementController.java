package com.course.controller;

import com.course.dto.AchievementDto;
import com.course.dto.CreateAchievementForm;
import com.course.dto.UpdateAchievementForm;
import com.course.dto.UpdateAchievementDto;
import com.course.service.AchievementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @PostMapping(value = "/api/courses/{courseId}/achievements", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<AchievementDto> create(
            @PathVariable Integer courseId,
            @Valid @ModelAttribute CreateAchievementForm form) {
        AchievementDto created = achievementService.create(courseId, form);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/courses/{courseId}/achievements")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<List<AchievementDto>> listByCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(achievementService.listByCourse(courseId));
    }

    @GetMapping("/api/achievements/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<AchievementDto> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(achievementService.getById(id));
    }

    @PutMapping(value = "/api/achievements/{id}", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<AchievementDto> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateAchievementDto dto) {
        return ResponseEntity.ok(achievementService.update(id, dto));
    }

    @PutMapping(value = "/api/achievements/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<AchievementDto> updateWithOptionalPhoto(
            @PathVariable Integer id,
            @Valid @ModelAttribute UpdateAchievementForm form) {
        return ResponseEntity.ok(achievementService.updateWithOptionalPhoto(id, form));
    }

    @PutMapping(value = "/api/achievements/{id}/photo", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<AchievementDto> replacePhoto(
            @PathVariable Integer id,
            @RequestPart("photo") MultipartFile photo) {
        return ResponseEntity.ok(achievementService.replacePhoto(id, photo));
    }

    @DeleteMapping("/api/achievements/{id}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        achievementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
