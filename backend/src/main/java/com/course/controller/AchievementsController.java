package com.course.controller;

import com.course.dto.achievement.AchievementResponse;
import com.course.dto.achievement.AchievementUpsertForm;
import com.course.dto.achievement.AchievementUpdateRequest;
import com.course.dto.achievement.StudentAchievementResponse;
import com.course.entity.Achievement;
import com.course.entity.RoleName;
import com.course.entity.User;
import com.course.exception.ForbiddenOperationException;
import com.course.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AchievementsController {

    private static final RoleName ROLE_TEACHER = RoleName.TEACHER;
    private static final RoleName ROLE_METHODIST = RoleName.METHODIST;

    private final AchievementService achievementService;
    private final StudentAchievementService studentAchievementService;
    private final AuthService authService;
    private final UserService userService;
    private final ClassStudentService classStudentService;

    

    @PostMapping(value = "/courses/{courseId}/achievements", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<AchievementResponse> create(
            @PathVariable Integer courseId,
            @Valid @ModelAttribute AchievementUpsertForm form
    ) {
        AchievementResponse created = achievementService.create(courseId, form);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/courses/{courseId}/achievements")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<List<AchievementResponse>> listByCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(achievementService.listByCourse(courseId));
    }

    @GetMapping("/achievements/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST','STUDENT')")
    public ResponseEntity<AchievementResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(achievementService.getById(id));
    }

    @PutMapping(value = "/achievements/{id}", consumes = {"application/json"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<AchievementResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody AchievementUpdateRequest dto
    ) {
        return ResponseEntity.ok(achievementService.update(id, dto));
    }

    @PutMapping(value = "/achievements/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<AchievementResponse> updateWithOptionalPhoto(
            @PathVariable Integer id,
            @Valid @ModelAttribute AchievementUpsertForm form
    ) {
        return ResponseEntity.ok(achievementService.updateWithOptionalPhoto(id, form));
    }

    @PutMapping(value = "/achievements/{id}/photo", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<AchievementResponse> replacePhoto(
            @PathVariable Integer id,
            @RequestPart("photo") MultipartFile photo
    ) {
        return ResponseEntity.ok(achievementService.replacePhoto(id, photo));
    }

    @DeleteMapping("/achievements/{id}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        achievementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    

    @PostMapping("/achievements/{achievementId}/award/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<StudentAchievementResponse> award(
            @PathVariable Integer achievementId,
            @PathVariable Integer studentId
    ) {
        Achievement achievement = achievementService.getEntityById(achievementId);
        return ResponseEntity.ok(studentAchievementService.awardToStudent(achievement, studentId));
    }

    @DeleteMapping("/achievements/{achievementId}/award/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<Void> revoke(
            @PathVariable Integer achievementId,
            @PathVariable Integer studentId
    ) {
        Achievement achievement = achievementService.getEntityById(achievementId);
        studentAchievementService.revokeFromStudent(achievement, studentId);
        return ResponseEntity.noContent().build();
    }

    

    @GetMapping("/students/{studentId}/achievements")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST')")
    public ResponseEntity<List<StudentAchievementResponse>> getStudentAchievements(@PathVariable Integer studentId) {
        User current = authService.getCurrentUserEntity();
        RoleName role = current != null && current.getRole() != null ? current.getRole().getRolename() : null;
        if (role == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }

        if (ROLE_TEACHER == role) {
            userService.assertUserEntityHasRole(current, ROLE_TEACHER);
            classStudentService.assertStudentInTeacherClasses(studentId, current.getId(),
                    "Teacher can view achievements only for own students");
        } else if (ROLE_METHODIST == role) {
            userService.assertUserEntityHasRole(current, ROLE_METHODIST);
            classStudentService.assertStudentInMethodistCourses(studentId, current.getId(),
                    "Methodist can view achievements only for students in own courses");
        }

        return ResponseEntity.ok(studentAchievementService.listByStudent(studentId));
    }
}
