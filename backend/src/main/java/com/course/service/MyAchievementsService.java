package com.course.service;

import com.course.dto.AchievementDto;
import com.course.dto.CourseDto;
import com.course.dto.MyAchievementsPageDto;
import com.course.dto.StudentAchievementDto;
import com.course.entity.RoleName;
import com.course.entity.User;
import com.course.repository.StudentAchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyAchievementsService {

    private static final RoleName ROLE_STUDENT = RoleName.STUDENT;

    private final AuthService authService;
    private final UserService userService;

    // own repository
    private final StudentAchievementRepository studentAchievementRepository;

    // cross-service calls
    private final StudentContentService studentContentService;
    private final AchievementService achievementService;
    private final StudentAchievementService studentAchievementService;

    public MyAchievementsPageDto getMyAchievementsPage() {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_STUDENT);

        // Earned achievements
        List<StudentAchievementDto> earned = studentAchievementRepository
                .findAllByStudent_IdOrderByAwardedAtDesc(current.getId())
                .stream()
                .map(studentAchievementService::toDto)
                .toList();

        // All achievements in student's courses (for total progress & recommendations)
        List<CourseDto> courses = studentContentService.listMyCourses();

        List<AchievementDto> allAvailable = new ArrayList<>();
        for (CourseDto c : courses) {
            if (c == null || c.getId() == null) continue;
            allAvailable.addAll(achievementService.listByCourse(c.getId()));
        }

        Set<Integer> earnedIds = new HashSet<>();
        for (StudentAchievementDto e : earned) {
            if (e.getAchievementId() != null) earnedIds.add(e.getAchievementId());
        }

        List<AchievementDto> notEarned = allAvailable.stream()
                .filter(a -> a != null && a.getId() != null && !earnedIds.contains(a.getId()))
                .toList();

        MyAchievementsPageDto dto = new MyAchievementsPageDto();
        dto.setTotalAvailable(allAvailable.size());
        dto.setTotalEarned(earned.size());
        dto.setEarned(earned);
        dto.setRecommendations(notEarned);
        return dto;
    }
}
