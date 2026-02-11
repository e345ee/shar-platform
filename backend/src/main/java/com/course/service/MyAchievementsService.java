package com.course.service;

import com.course.dto.achievement.AchievementResponse;
import com.course.dto.achievement.MyAchievementsPageResponse;
import com.course.dto.achievement.StudentAchievementResponse;
import com.course.dto.course.CourseResponse;
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

    
    private final StudentAchievementRepository studentAchievementRepository;

    
    private final StudentContentService studentContentService;
    private final AchievementService achievementService;
    private final StudentAchievementService studentAchievementService;

    public MyAchievementsPageResponse getMyAchievementsPage() {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_STUDENT);

        
        List<StudentAchievementResponse> earned = studentAchievementRepository
                .findAllByStudent_IdOrderByAwardedAtDesc(current.getId())
                .stream()
                .map(studentAchievementService::toDto)
                .toList();

        
        List<CourseResponse> courses = studentContentService.listMyCourses();

        List<AchievementResponse> allAvailable = new ArrayList<>();
        for (CourseResponse c : courses) {
            if (c == null || c.getId() == null) continue;
            allAvailable.addAll(achievementService.listByCourse(c.getId()));
        }

        Set<Integer> earnedIds = new HashSet<>();
        for (StudentAchievementResponse e : earned) {
            if (e.getAchievementId() != null) earnedIds.add(e.getAchievementId());
        }

        List<AchievementResponse> notEarned = allAvailable.stream()
                .filter(a -> a != null && a.getId() != null && !earnedIds.contains(a.getId()))
                .toList();

        MyAchievementsPageResponse dto = new MyAchievementsPageResponse();
        dto.setTotalAvailable(allAvailable.size());
        dto.setTotalEarned(earned.size());
        dto.setEarned(earned);
        dto.setRecommendations(notEarned);
        return dto;
    }
}
