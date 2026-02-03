package com.course.service;

import com.course.dto.AchievementDto;
import com.course.dto.CreateAchievementForm;
import com.course.dto.UpdateAchievementForm;
import com.course.dto.UpdateAchievementDto;
import com.course.entity.Achievement;
import com.course.entity.Course;
import com.course.entity.User;
import com.course.exception.AchievementAccessDeniedException;
import com.course.exception.AchievementNotFoundException;
import com.course.exception.AchievementValidationException;
import com.course.exception.DuplicateResourceException;
import com.course.repository.AchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AchievementService {

    private static final String ROLE_METHODIST = "METHODIST";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_STUDENT = "STUDENT";

    private final AchievementRepository achievementRepository;
    private final CourseService courseService;
    private final UserService userService;
    private final AuthService authService;
    private final ClassStudentService classStudentService;
    private final StudyClassService studyClassService;
    private final AchievementPhotoStorageService photoStorageService;
    private final StudentAchievementService studentAchievementService;

    public AchievementDto create(Integer courseId, CreateAchievementForm form) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Course course = courseService.getEntityById(courseId);
        assertOwner(course.getCreatedBy(), current, "Only course creator can create achievements");

        if (form == null) {
            throw new AchievementValidationException("Achievement data is required");
        }

        String title = safeTrim(form.getTitle());
        String joke = safeTrim(form.getJokeDescription());
        String desc = safeTrim(form.getDescription());
        MultipartFile photo = form.getPhoto();

        if (!StringUtils.hasText(title) || !StringUtils.hasText(joke) || !StringUtils.hasText(desc)) {
            throw new AchievementValidationException("Achievement fields must not be empty");
        }

        if (achievementRepository.existsByCourse_IdAndTitleIgnoreCase(courseId, title)) {
            throw new DuplicateResourceException("Achievement with title '" + title + "' already exists in this course");
        }

        String photoUrl = photoStorageService.uploadAchievementPhoto(courseId, photo);

        Achievement a = new Achievement();
        a.setCourse(course);
        a.setCreatedBy(current);
        a.setTitle(title);
        a.setJokeDescription(joke);
        a.setDescription(desc);
        a.setPhotoUrl(photoUrl);

        return toDto(achievementRepository.save(a));
    }

    @Transactional(readOnly = true)
    public AchievementDto getById(Integer id) {
        Achievement a = getEntityById(id);
        assertCanViewAchievement(authService.getCurrentUserEntity(), a);
        return toDto(a);
    }

    @Transactional(readOnly = true)
    public Achievement getEntityById(Integer id) {
        return achievementRepository.findById(id)
                .orElseThrow(() -> new AchievementNotFoundException("Achievement with id " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<AchievementDto> listByCourse(Integer courseId) {
        Course course = courseService.getEntityById(courseId);
        User current = authService.getCurrentUserEntity();

        if (isRole(current, ROLE_METHODIST)) {
            User owner = course.getCreatedBy();
            if (owner == null || owner.getId() == null || current.getId() == null || !owner.getId().equals(current.getId())) {
                throw new AchievementAccessDeniedException("Methodist can access only own courses");
            }
        } else if (isRole(current, ROLE_TEACHER)) {
            studyClassService.assertTeacherCanManageCourse(courseId, current);
        } else if (isRole(current, ROLE_STUDENT)) {
            classStudentService.assertStudentInCourse(current.getId(), courseId, "Student does not belong to this course");
        }
        return achievementRepository.findAllByCourse_IdOrderByCreatedAtDesc(courseId)
                .stream().map(this::toDto).toList();
    }

    public AchievementDto update(Integer id, UpdateAchievementDto dto) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Achievement a = getEntityById(id);
        assertOwner(a.getCreatedBy(), current, "Only achievement creator can edit this achievement");

        if (dto == null) {
            throw new AchievementValidationException("Achievement data is required");
        }

        String title = safeTrim(dto.getTitle());
        String joke = safeTrim(dto.getJokeDescription());
        String desc = safeTrim(dto.getDescription());

        if (!StringUtils.hasText(title) || !StringUtils.hasText(joke) || !StringUtils.hasText(desc)) {
            throw new AchievementValidationException("Achievement fields must not be empty");
        }

        Integer courseId = a.getCourse() != null ? a.getCourse().getId() : null;
        if (courseId != null && !title.equalsIgnoreCase(a.getTitle())
                && achievementRepository.existsByCourse_IdAndTitleIgnoreCase(courseId, title)) {
            throw new DuplicateResourceException("Achievement with title '" + title + "' already exists in this course");
        }

        a.setTitle(title);
        a.setJokeDescription(joke);
        a.setDescription(desc);

        return toDto(achievementRepository.save(a));
    }

    public AchievementDto updateWithOptionalPhoto(Integer id, UpdateAchievementForm form) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Achievement a = getEntityById(id);
        assertOwner(a.getCreatedBy(), current, "Only achievement creator can edit this achievement");

        if (form == null) {
            throw new AchievementValidationException("Achievement data is required");
        }

        String title = safeTrim(form.getTitle());
        String joke = safeTrim(form.getJokeDescription());
        String desc = safeTrim(form.getDescription());

        if (!StringUtils.hasText(title) || !StringUtils.hasText(joke) || !StringUtils.hasText(desc)) {
            throw new AchievementValidationException("Achievement fields must not be empty");
        }

        Integer courseId = a.getCourse() != null ? a.getCourse().getId() : null;
        if (courseId != null && !title.equalsIgnoreCase(a.getTitle())
                && achievementRepository.existsByCourse_IdAndTitleIgnoreCase(courseId, title)) {
            throw new DuplicateResourceException("Achievement with title '" + title + "' already exists in this course");
        }

        a.setTitle(title);
        a.setJokeDescription(joke);
        a.setDescription(desc);

        String oldUrl = a.getPhotoUrl();
        if (form.getPhoto() != null && !form.getPhoto().isEmpty()) {
            if (courseId == null) {
                throw new AchievementValidationException("Achievement course is missing");
            }
            String newUrl = photoStorageService.uploadAchievementPhoto(courseId, form.getPhoto());
            a.setPhotoUrl(newUrl);
        }

        Achievement saved = achievementRepository.save(a);

        // if photo was replaced successfully, cleanup old one
        if (form.getPhoto() != null && !form.getPhoto().isEmpty()) {
            photoStorageService.deleteByPublicUrl(oldUrl);
        }

        return toDto(saved);
    }

    public AchievementDto replacePhoto(Integer id, MultipartFile photo) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Achievement a = getEntityById(id);
        assertOwner(a.getCreatedBy(), current, "Only achievement creator can change achievement photo");

        Integer courseId = a.getCourse() != null ? a.getCourse().getId() : null;
        if (courseId == null) {
            throw new AchievementValidationException("Achievement course is missing");
        }

        String oldUrl = a.getPhotoUrl();
        String newUrl = photoStorageService.uploadAchievementPhoto(courseId, photo);
        a.setPhotoUrl(newUrl);
        Achievement saved = achievementRepository.save(a);

        photoStorageService.deleteByPublicUrl(oldUrl);
        return toDto(saved);
    }

    public void delete(Integer id) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Achievement a = getEntityById(id);
        assertOwner(a.getCreatedBy(), current, "Only achievement creator can delete this achievement");

        String oldUrl = a.getPhotoUrl();

        // Make deletion robust even if DB doesn't have ON DELETE CASCADE for student_achievements
        studentAchievementService.deleteAllForAchievement(id);

        achievementRepository.delete(a);
        photoStorageService.deleteByPublicUrl(oldUrl);
    }

    public AchievementDto toDto(Achievement a) {
        AchievementDto dto = new AchievementDto();
        dto.setId(a.getId());
        dto.setTitle(a.getTitle());
        dto.setJokeDescription(a.getJokeDescription());
        dto.setDescription(a.getDescription());
        dto.setPhotoUrl(a.getPhotoUrl());

        if (a.getCourse() != null) {
            dto.setCourseId(a.getCourse().getId());
        }

        if (a.getCreatedBy() != null) {
            dto.setCreatedById(a.getCreatedBy().getId());
            dto.setCreatedByName(a.getCreatedBy().getName());
        }

        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        return dto;
    }

    private void assertOwner(User owner, User current, String message) {
        if (owner == null || owner.getId() == null || current == null || current.getId() == null
                || !owner.getId().equals(current.getId())) {
            throw new AchievementAccessDeniedException(message);
        }
    }

    private void assertCanViewAchievement(User current, Achievement a) {
        if (current == null || a == null || a.getCourse() == null) {
            throw new AchievementAccessDeniedException("Forbidden");
        }

        Integer courseId = a.getCourse().getId();

        if (isRole(current, ROLE_METHODIST)) {
            User owner = a.getCourse().getCreatedBy();
            if (owner == null || owner.getId() == null || current.getId() == null || !owner.getId().equals(current.getId())) {
                throw new AchievementAccessDeniedException("Methodist can access only own courses");
            }
        } else if (isRole(current, ROLE_TEACHER)) {
            studyClassService.assertTeacherCanManageCourse(courseId, current);
        } else if (isRole(current, ROLE_STUDENT)) {
            classStudentService.assertStudentInCourse(current.getId(), courseId, "Student does not belong to this course");
        }
    }

    private boolean isRole(User user, String role) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRolename() != null
                && role.equalsIgnoreCase(user.getRole().getRolename());
    }

    private String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
}
