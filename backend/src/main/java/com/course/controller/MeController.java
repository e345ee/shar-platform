package com.course.controller;

import com.course.dto.activity.ActivityResponse;
import com.course.dto.achievement.MyAchievementsPageResponse;
import com.course.dto.achievement.StudentAchievementResponse;
import com.course.dto.attempt.AttemptResponse;
import com.course.dto.auth.ChangePasswordRequest;
import com.course.dto.course.CourseResponse;
import com.course.dto.course.StudentCoursePageResponse;
import com.course.dto.lesson.LessonResponse;
import com.course.dto.notification.NotificationResponse;
import com.course.dto.common.PageResponse;
import com.course.dto.statistics.StudentStatisticsOverviewResponse;
import com.course.dto.statistics.StudentTopicStatsResponse;
import com.course.dto.user.ProfileUpdateRequest;
import com.course.dto.user.UserResponse;
import com.course.entity.User;
import com.course.exception.ResourceNotFoundException;
import com.course.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Pageable;

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

    

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getMe() {
        User current = authService.getCurrentUserEntity();
        return ResponseEntity.ok(userService.getUserById(current.getId()));
    }

    @PatchMapping(value = "/profile", consumes = {"application/json"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest dto) {
        User current = authService.getCurrentUserEntity();
        return ResponseEntity.ok(userService.updateOwnProfile(current, dto));
    }

    @PutMapping(value = "/password", consumes = {"application/json"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest dto) {
        User current = authService.getCurrentUserEntity();
        userService.changeOwnPassword(current, dto.getCurrentPassword(), dto.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/avatar", consumes = {"multipart/form-data"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> uploadAvatar(@RequestPart("file") MultipartFile file) {
        User current = authService.getCurrentUserEntity();
        return ResponseEntity.ok(userService.uploadOwnAvatar(current, file));
    }

    @DeleteMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> deleteAvatar() {
        User current = authService.getCurrentUserEntity();
        return ResponseEntity.ok(userService.deleteOwnAvatar(current));
    }

    

    @GetMapping("/notifications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<NotificationResponse>> listNotifications(Pageable pageable) {
        return ResponseEntity.ok(notificationService.listMyNotifications(pageable));
    }

    @GetMapping("/notifications/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("unread", notificationService.countMyUnread()));
    }

    @PatchMapping("/notifications/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable Integer id) {
        return ResponseEntity.ok(notificationService.markRead(id));
    }

    @PatchMapping("/notifications/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Integer>> markAllRead() {
        return ResponseEntity.ok(Map.of("marked", notificationService.markAllRead()));
    }

    

    @GetMapping("/courses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<CourseResponse>> listMyCourses() {
        return ResponseEntity.ok(studentContentService.listMyCourses());
    }

    @GetMapping("/courses/{courseId}/page")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentCoursePageResponse> getCoursePage(@PathVariable Integer courseId) {
        return ResponseEntity.ok(studentCoursePageService.getCoursePage(courseId));
    }

    @GetMapping("/courses/{courseId}/lessons")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<LessonResponse>> listMyLessonsInCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(studentContentService.listMyLessonsInCourse(courseId));
    }

    @GetMapping("/activities/{activityId}/attempts/latest")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AttemptResponse> getLatestCompletedAttempt(@PathVariable Integer activityId) {
        return ResponseEntity.ok(testAttemptService.getLatestCompletedAttemptForTest(activityId));
    }

    @GetMapping("/lessons/{lessonId}/activity/attempts/latest")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AttemptResponse> getLatestCompletedAttemptByLesson(@PathVariable Integer lessonId) {
        List<ActivityResponse> tests = testService.listByLesson(lessonId);
        if (tests == null || tests.isEmpty()) {
            throw new ResourceNotFoundException("No activity for lesson " + lessonId);
        }
        Integer testId = tests.get(0).getId();
        return ResponseEntity.ok(testAttemptService.getLatestCompletedAttemptForTest(testId));
    }

    

    @PostMapping("/courses/{courseId}/completion-email")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> sendCompletionEmail(@PathVariable Integer courseId) {
        courseCompletionEmailService.sendMyCompletionEmail(courseId);
        return ResponseEntity.ok().build();
    }

    

    @GetMapping("/achievements")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentAchievementResponse>> getMyAchievements() {
        return ResponseEntity.ok(studentAchievementService.getMyAchievements());
    }

    @GetMapping("/achievements/page")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<MyAchievementsPageResponse> getMyAchievementsPage() {
        return ResponseEntity.ok(myAchievementsService.getMyAchievementsPage());
    }

    

    @GetMapping("/statistics/overview")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentStatisticsOverviewResponse> overview() {
        return ResponseEntity.ok(statisticsService.getMyOverview());
    }

    @GetMapping("/statistics/topics")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentTopicStatsResponse>> topics(@RequestParam(required = false) Integer courseId) {
        return ResponseEntity.ok(statisticsService.getMyTopicStats(courseId));
    }
}
