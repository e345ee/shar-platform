package com.course.service;

import com.course.dto.achievement.StudentAchievementResponse;
import com.course.entity.Achievement;
import com.course.entity.NotificationType;
import com.course.entity.RoleName;
import com.course.entity.StudentAchievement;
import com.course.entity.User;
import com.course.exception.AchievementAlreadyAwardedException;
import com.course.exception.AchievementNotFoundException;
import com.course.exception.ResourceNotFoundException;
import com.course.repository.StudentAchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentAchievementService {

    private static final RoleName ROLE_TEACHER = RoleName.TEACHER;
    private static final RoleName ROLE_METHODIST = RoleName.METHODIST;
    private static final RoleName ROLE_STUDENT = RoleName.STUDENT;

    private final StudentAchievementRepository studentAchievementRepository;
    private final StudyClassService classService;
    private final ClassStudentService classStudentService;
    private final UserService userService;
    private final AuthService authService;

    private final NotificationService notificationService;

    private final ClassAchievementFeedService classAchievementFeedService;

    public StudentAchievementResponse awardToStudent(Achievement achievement, Integer studentId) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasAnyRole(current, ROLE_TEACHER, ROLE_METHODIST);

        if (achievement == null || achievement.getId() == null) {
            throw new AchievementNotFoundException("Achievement not found");
        }
        Integer achievementId = achievement.getId();
        if (achievement.getCourse() == null || achievement.getCourse().getId() == null) {
            throw new AchievementNotFoundException("Achievement course is missing");
        }

        if (current.getRole().getRolename() == ROLE_TEACHER) {
            classService.assertTeacherCanManageCourse(achievement.getCourse().getId(), current);
        } else {
            
            if (achievement.getCourse().getCreatedBy() == null
                    || achievement.getCourse().getCreatedBy().getId() == null
                    || !achievement.getCourse().getCreatedBy().getId().equals(current.getId())) {
                throw new com.course.exception.ForbiddenOperationException("Methodist can award achievements only in own courses");
            }
        }

        User student = userService.getUserEntityById(studentId);
        userService.assertUserEntityHasRole(student, ROLE_STUDENT);

        if (current.getRole().getRolename() == ROLE_TEACHER) {
            classStudentService.assertStudentInTeacherCourse(studentId, current.getId(), achievement.getCourse().getId(),
                    "Student is not enrolled in teacher's classes for this course");
        } else {
            classStudentService.assertStudentInCourse(studentId, achievement.getCourse().getId(),
                    "Student is not enrolled in the course");
        }

        if (studentAchievementRepository.existsByStudent_IdAndAchievement_Id(studentId, achievementId)) {
            throw new AchievementAlreadyAwardedException("Achievement already awarded to this student");
        }

        StudentAchievement sa = new StudentAchievement();
        sa.setStudent(student);
        sa.setAchievement(achievement);
        sa.setAwardedBy(current);

        StudentAchievement saved = studentAchievementRepository.save(sa);

        
        classAchievementFeedService.publishAward(saved);

        
        notificationService.create(student,
                NotificationType.ACHIEVEMENT_AWARDED,
                "Новая ачивка",
                "Вам назначена ачивка: " + achievement.getTitle(),
                achievement.getCourse().getId(),
                null,
                null,
                null,
                achievementId);

        return toDto(saved);
    }

    public void revokeFromStudent(Achievement achievement, Integer studentId) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasAnyRole(current, ROLE_TEACHER, ROLE_METHODIST);

        if (achievement == null || achievement.getId() == null) {
            throw new AchievementNotFoundException("Achievement not found");
        }
        Integer achievementId = achievement.getId();
        if (achievement.getCourse() == null || achievement.getCourse().getId() == null) {
            throw new AchievementNotFoundException("Achievement course is missing");
        }

        if (current.getRole().getRolename() == ROLE_TEACHER) {
            classService.assertTeacherCanManageCourse(achievement.getCourse().getId(), current);
        } else {
            if (achievement.getCourse().getCreatedBy() == null
                    || achievement.getCourse().getCreatedBy().getId() == null
                    || !achievement.getCourse().getCreatedBy().getId().equals(current.getId())) {
                throw new com.course.exception.ForbiddenOperationException("Methodist can revoke achievements only in own courses");
            }
        }

        StudentAchievement sa = studentAchievementRepository.findByStudent_IdAndAchievement_Id(studentId, achievementId)
                .orElseThrow(() -> new ResourceNotFoundException("Award record not found"));

        studentAchievementRepository.delete(sa);
    }

    @Transactional(readOnly = true)
    public List<StudentAchievementResponse> getMyAchievements() {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_STUDENT);
        return studentAchievementRepository.findAllByStudent_IdOrderByAwardedAtDesc(current.getId())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<StudentAchievementResponse> listByStudent(Integer studentId) {
        
        
        return studentAchievementRepository.findAllByStudent_IdOrderByAwardedAtDesc(studentId)
                .stream().map(this::toDto).toList();
    }

    
    public void deleteAllForAchievement(Integer achievementId) {
        if (achievementId == null) {
            return;
        }
        studentAchievementRepository.deleteAllByAchievement_Id(achievementId);
    }

    public StudentAchievementResponse toDto(StudentAchievement sa) {
        StudentAchievementResponse dto = new StudentAchievementResponse();
        dto.setId(sa.getId());
        dto.setAwardedAt(sa.getAwardedAt());

        if (sa.getStudent() != null) {
            dto.setStudentId(sa.getStudent().getId());
            dto.setStudentName(sa.getStudent().getName());
        }

        if (sa.getAchievement() != null) {
            dto.setAchievementId(sa.getAchievement().getId());
            dto.setAchievementTitle(sa.getAchievement().getTitle());
            dto.setAchievementPhotoUrl(sa.getAchievement().getPhotoUrl());

            dto.setAchievementJokeDescription(sa.getAchievement().getJokeDescription());
            dto.setAchievementDescription(sa.getAchievement().getDescription());

            if (sa.getAchievement().getCourse() != null) {
                dto.setAchievementCourseId(sa.getAchievement().getCourse().getId());
            }
        }

        if (sa.getAwardedBy() != null) {
            dto.setAwardedById(sa.getAwardedBy().getId());
            dto.setAwardedByName(sa.getAwardedBy().getName());
        }

        return dto;
    }
}
