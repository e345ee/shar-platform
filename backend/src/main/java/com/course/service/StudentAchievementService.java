package com.course.service;

import com.course.dto.StudentAchievementDto;
import com.course.entity.Achievement;
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
    private static final RoleName ROLE_STUDENT = RoleName.STUDENT;

    private final StudentAchievementRepository studentAchievementRepository;
    private final StudyClassService classService;
    private final ClassStudentService classStudentService;
    private final UserService userService;
    private final AuthService authService;

    private final ClassAchievementFeedService classAchievementFeedService;

    public StudentAchievementDto awardToStudent(Achievement achievement, Integer studentId) {
        User teacher = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(teacher, ROLE_TEACHER);

        if (achievement == null || achievement.getId() == null) {
            throw new AchievementNotFoundException("Achievement not found");
        }
        Integer achievementId = achievement.getId();
        if (achievement.getCourse() == null || achievement.getCourse().getId() == null) {
            throw new AchievementNotFoundException("Achievement course is missing");
        }

        classService.assertTeacherCanManageCourse(achievement.getCourse().getId(), teacher);

        User student = userService.getUserEntityById(studentId);
        userService.assertUserEntityHasRole(student, ROLE_STUDENT);

        classStudentService.assertStudentInTeacherCourse(studentId, teacher.getId(), achievement.getCourse().getId(),
                "Student is not enrolled in teacher's classes for this course");

        if (studentAchievementRepository.existsByStudent_IdAndAchievement_Id(studentId, achievementId)) {
            throw new AchievementAlreadyAwardedException("Achievement already awarded to this student");
        }

        StudentAchievement sa = new StudentAchievement();
        sa.setStudent(student);
        sa.setAchievement(achievement);
        sa.setAwardedBy(teacher);

        StudentAchievement saved = studentAchievementRepository.save(sa);

        // Publish to all class feeds where the student is enrolled for this course.
        classAchievementFeedService.publishAward(saved);

        return toDto(saved);
    }

    public void revokeFromStudent(Achievement achievement, Integer studentId) {
        User teacher = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(teacher, ROLE_TEACHER);

        if (achievement == null || achievement.getId() == null) {
            throw new AchievementNotFoundException("Achievement not found");
        }
        Integer achievementId = achievement.getId();
        if (achievement.getCourse() == null || achievement.getCourse().getId() == null) {
            throw new AchievementNotFoundException("Achievement course is missing");
        }

        classService.assertTeacherCanManageCourse(achievement.getCourse().getId(), teacher);

        StudentAchievement sa = studentAchievementRepository.findByStudent_IdAndAchievement_Id(studentId, achievementId)
                .orElseThrow(() -> new ResourceNotFoundException("Award record not found"));

        studentAchievementRepository.delete(sa);
    }

    @Transactional(readOnly = true)
    public List<StudentAchievementDto> getMyAchievements() {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_STUDENT);
        return studentAchievementRepository.findAllByStudent_IdOrderByAwardedAtDesc(current.getId())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<StudentAchievementDto> listByStudent(Integer studentId) {
        // Teacher/Methodist access control should be done in controller or via dedicated service,
        // this method returns data only.
        return studentAchievementRepository.findAllByStudent_IdOrderByAwardedAtDesc(studentId)
                .stream().map(this::toDto).toList();
    }

    /**
     * Used when an achievement is removed from the system.
     * We delete award records explicitly to be compatible with databases
     * where foreign keys might not have ON DELETE CASCADE.
     */
    public void deleteAllForAchievement(Integer achievementId) {
        if (achievementId == null) {
            return;
        }
        studentAchievementRepository.deleteAllByAchievement_Id(achievementId);
    }

    public StudentAchievementDto toDto(StudentAchievement sa) {
        StudentAchievementDto dto = new StudentAchievementDto();
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
