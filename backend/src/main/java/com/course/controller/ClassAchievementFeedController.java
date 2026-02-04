package com.course.controller;

import com.course.dto.StudentAchievementDto;
import com.course.entity.RoleName;
import com.course.entity.User;
import com.course.exception.ForbiddenOperationException;
import com.course.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ClassAchievementFeedController {

    private final ClassAchievementFeedService feedService;
    private final AuthService authService;
    private final UserService userService;
    private final ClassStudentService classStudentService;
    private final StudyClassService studyClassService;

    /**
     * Class feed of awarded achievements.
     * Students see feed only for classes they are enrolled in.
     * Teachers/Methodists see feed only for their own classes.
     */
    @GetMapping("/api/classes/{classId}/achievement-feed")
    @PreAuthorize("hasAnyRole('ADMIN','METHODIST','TEACHER','STUDENT')")
    public ResponseEntity<List<StudentAchievementDto>> getClassAchievementFeed(@PathVariable Integer classId) {
        User current = authService.getCurrentUserEntity();
        RoleName role = current != null && current.getRole() != null ? current.getRole().getRolename() : null;
        if (role == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }

        if (RoleName.STUDENT == role) {
            userService.assertUserEntityHasRole(current, RoleName.STUDENT);
            classStudentService.assertStudentInClass(current.getId(), classId, "Student can view feed only for own classes");
        } else if (RoleName.TEACHER == role || RoleName.METHODIST == role) {
            // Will throw if class does not belong to the teacher/methodist
            studyClassService.getMyClassById(classId);
        }
        // ADMIN can view any class

        return ResponseEntity.ok(feedService.getFeedForClass(classId));
    }
}
