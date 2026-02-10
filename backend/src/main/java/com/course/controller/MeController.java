package com.course.controller;

import com.course.dto.*;
import com.course.entity.User;
import com.course.exception.ResourceNotFoundException;
import com.course.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final AuthService authService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final StudentContentService studentContentService;
    private final StudentCoursePageService studentCoursePageService;
    private final TestAttemptService testAttemptService;
    private final TestService testService;
    private final CourseCompletionEmailService courseCompletionEmailService;
    private final StatisticsService statisticsService;
    private final StudentAchievementService studentAchievementService;
    private final MyAchievementsService myAchievementsService;

    // --- Profile ---

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getMe() {
        User current = authService.getCurrentUserEntity();
        return ResponseEntity.ok(userService.getUserById(current.getId()));
    }

    @PatchMapping(value = "/profile", consumes = {"application/json"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> updateProfile(@Valid @RequestBody UpdateProfileDto dto) {
        User current = authService.getCurrentUserEntity();
        return ResponseEntity.ok(userService.updateOwnProfile(current, dto));
    }

    @PutMapping(value = "/password", consumes = {"application/json"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordDto dto) {
        User current = authService.getCurrentUserEntity();
        userService.changeOwnPassword(current, dto.getCurrentPassword(), dto.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/avatar", consumes = {"multipart/form-data"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> uploadAvatar(@RequestPart("file") MultipartFile file) {
        User current = authService.getCurrentUserEntity();
        return ResponseEntity.ok(userService.uploadOwnAvatar(current, file));
    }

    @DeleteMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> deleteAvatar() {
        User current = authService.getCurrentUserEntity();
        return ResponseEntity.ok(userService.deleteOwnAvatar(current));
    }

    // --- Notifications ---

    @GetMapping("/notifications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationDto>> listNotifications() {
        return ResponseEntity.ok(notificationService.listMyNotifications());
    }

    @GetMapping("/notifications/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("unread", notificationService.countMyUnread()));
    }

    @PatchMapping("/notifications/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationDto> markRead(@PathVariable Integer id) {
        return ResponseEntity.ok(notificationService.markRead(id));
    }

    @PatchMapping("/notifications/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Integer>> markAllRead() {
        return ResponseEntity.ok(Map.of("marked", notificationService.markAllRead()));
    }

    // --- Student content ---

    @GetMapping("/courses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<CourseDto>> listMyCourses() {
        return ResponseEntity.ok(studentContentService.listMyCourses());
    }

    @GetMapping("/courses/{courseId}/page")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentCoursePageDto> getCoursePage(@PathVariable Integer courseId) {
        return ResponseEntity.ok(studentCoursePageService.getCoursePage(courseId));
    }

    @GetMapping("/courses/{courseId}/lessons")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<LessonDto>> listMyLessonsInCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(studentContentService.listMyLessonsInCourse(courseId));
    }

    @GetMapping("/activities/{activityId}/attempts/latest")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TestAttemptDto> getLatestCompletedAttempt(@PathVariable Integer activityId) {
        return ResponseEntity.ok(testAttemptService.getLatestCompletedAttemptForTest(activityId));
    }

    @GetMapping("/lessons/{lessonId}/activity/attempts/latest")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<TestAttemptDto> getLatestCompletedAttemptByLesson(@PathVariable Integer lessonId) {
        List<TestSummaryDto> tests = testService.listByLesson(lessonId);
        if (tests == null || tests.isEmpty()) {
            throw new ResourceNotFoundException("No activity for lesson " + lessonId);
        }
        Integer testId = tests.get(0).getId();
        return ResponseEntity.ok(testAttemptService.getLatestCompletedAttemptForTest(testId));
    }

    // --- Certificates (student only) ---

    @PostMapping("/courses/{courseId}/completion-email")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> sendCompletionEmail(@PathVariable Integer courseId) {
        courseCompletionEmailService.sendMyCompletionEmail(courseId);
        return ResponseEntity.ok().build();
    }

    // --- Student achievements ---

    @GetMapping("/achievements")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentAchievementDto>> getMyAchievements() {
        return ResponseEntity.ok(studentAchievementService.getMyAchievements());
    }

    @GetMapping("/achievements/page")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<MyAchievementsPageDto> getMyAchievementsPage() {
        return ResponseEntity.ok(myAchievementsService.getMyAchievementsPage());
    }

    // --- Student statistics ---

    @GetMapping("/statistics/overview")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentStatisticsOverviewDto> overview() {
        return ResponseEntity.ok(statisticsService.getMyOverview());
    }

    @GetMapping("/statistics/topics")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentTopicStatsDto>> topics(@RequestParam(required = false) Integer courseId) {
        return ResponseEntity.ok(statisticsService.getMyTopicStats(courseId));
    }
}
