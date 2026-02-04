package com.course.controller;

import com.course.dto.StudentAchievementDto;
import com.course.dto.MyAchievementsPageDto;
import com.course.entity.Achievement;
import com.course.entity.RoleName;
import com.course.entity.User;
import com.course.exception.ForbiddenOperationException;
import com.course.service.AuthService;
import com.course.service.ClassStudentService;
import com.course.service.AchievementService;
import com.course.service.StudentAchievementService;
import com.course.service.MyAchievementsService;
import com.course.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StudentAchievementController {
    private static final RoleName ROLE_TEACHER = RoleName.TEACHER;
    private static final RoleName ROLE_METHODIST = RoleName.METHODIST;

    private final StudentAchievementService studentAchievementService;
    private final AchievementService achievementService;
    private final AuthService authService;
    private final UserService userService;
    private final ClassStudentService classStudentService;
    private final MyAchievementsService myAchievementsService;

    @PostMapping("/api/achievements/{achievementId}/award/{studentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<StudentAchievementDto> award(
            @PathVariable Integer achievementId,
            @PathVariable Integer studentId) {
        Achievement achievement = achievementService.getEntityById(achievementId);
        return ResponseEntity.ok(studentAchievementService.awardToStudent(achievement, studentId));
    }

    @DeleteMapping("/api/achievements/{achievementId}/award/{studentId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> revoke(
            @PathVariable Integer achievementId,
            @PathVariable Integer studentId) {
        Achievement achievement = achievementService.getEntityById(achievementId);
        studentAchievementService.revokeFromStudent(achievement, studentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/users/me/achievements")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentAchievementDto>> getMyAchievements() {
        return ResponseEntity.ok(studentAchievementService.getMyAchievements());
    }

    /**
     * "My achievements" page:
     * - earned achievements (with conditions)
     * - overall progress (earned/available)
     * - recommendations (not yet earned)
     */
    @GetMapping("/api/users/me/achievements/page")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<MyAchievementsPageDto> getMyAchievementsPage() {
        return ResponseEntity.ok(myAchievementsService.getMyAchievementsPage());
    }

    /**
     * View achievements of a specific student.
     *
     * Access rules:
     * - ADMIN: can view any student
     * - TEACHER: can view only students from own classes
     * - METHODIST: can view students who are enrolled in methodist's courses
     */
    @GetMapping("/api/students/{studentId}/achievements")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','METHODIST')")
    public ResponseEntity<List<StudentAchievementDto>> getStudentAchievements(@PathVariable Integer studentId) {
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
