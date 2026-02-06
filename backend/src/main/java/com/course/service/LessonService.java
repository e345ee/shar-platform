package com.course.service;

import com.course.dto.LessonForm;
import com.course.dto.LessonDto;
import com.course.dto.UpdateLessonDto;
import com.course.entity.Course;
import com.course.entity.Lesson;
import com.course.entity.RoleName;
import com.course.entity.User;
import com.course.exception.DuplicateResourceException;
import com.course.exception.LessonAccessDeniedException;
import com.course.exception.LessonNotFoundException;
import com.course.exception.LessonValidationException;
import com.course.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LessonService {

    private static final RoleName ROLE_METHODIST = RoleName.METHODIST;
    private static final RoleName ROLE_TEACHER = RoleName.TEACHER;
    private static final RoleName ROLE_STUDENT = RoleName.STUDENT;

    private final LessonRepository lessonRepository;
    private final CourseService courseService;
    private final UserService userService;
    private final AuthService authService;
    private final ClassStudentService classStudentService;
    private final ClassOpenedLessonService classOpenedLessonService;
    private final StudyClassService studyClassService;
    private final LessonPresentationStorageService presentationStorageService;

    public LessonDto create(Integer courseId, LessonForm form) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Course course = courseService.getEntityById(courseId);
        assertOwner(course.getCreatedBy(), current, "Only course creator can create lessons");

        if (form == null) {
            throw new LessonValidationException("Lesson data is required");
        }

        String title = safeTrim(form.getTitle());
        String description = safeTrim(form.getDescription());

        if (!StringUtils.hasText(title)) {
            throw new LessonValidationException("Lesson title must not be empty");
        }

        if (lessonRepository.existsByCourse_IdAndTitleIgnoreCase(courseId, title)) {
            throw new DuplicateResourceException("Lesson with title '" + title + "' already exists in this course");
        }

        String url = presentationStorageService.uploadPresentation(courseId, form.getPresentation());

        
        int maxOrder = lessonRepository.findMaxOrderIndexInCourse(courseId);
        int appendIndex = maxOrder + 1;

        Lesson lesson = new Lesson();
        lesson.setCourse(course);
        lesson.setCreatedBy(current);
        lesson.setTitle(title);
        lesson.setDescription(description);
        lesson.setPresentationUrl(url);
        lesson.setOrderIndex(appendIndex);

        Lesson saved = lessonRepository.save(lesson);

        Integer desired = form.getOrderIndex();
        if (desired != null && desired > 0 && desired != appendIndex) {
            reorderWithinCourse(courseId, saved.getId(), desired);
            saved = getEntityById(saved.getId());
        }

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public LessonDto getById(Integer id) {
        return toDto(getEntityByIdForCurrentUser(id));
    }

    @Transactional(readOnly = true)
    public Lesson getEntityByIdForCurrentUser(Integer id) {
        Lesson lesson = getEntityById(id);
        User current = authService.getCurrentUserEntity();
        assertCanViewLesson(current, lesson);
        return lesson;
    }

    @Transactional(readOnly = true)
    public Lesson getEntityById(Integer id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new LessonNotFoundException("Lesson with id " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<LessonDto> listByCourse(Integer courseId) {
        Course course = courseService.getEntityById(courseId);

        User current = authService.getCurrentUserEntity();
        if (isRole(current, ROLE_METHODIST)) {
            User owner = course.getCreatedBy();
            if (owner == null || owner.getId() == null || current.getId() == null || !owner.getId().equals(current.getId())) {
                throw new LessonAccessDeniedException("Methodist can access only own courses");
            }
        } else if (isRole(current, ROLE_TEACHER)) {
            studyClassService.assertTeacherCanManageCourse(courseId, current);
        } else if (isRole(current, ROLE_STUDENT)) {
            classStudentService.assertStudentInCourse(
                    current.getId(),
                    courseId,
                    "Student does not belong to this course"
            );
        }

        return lessonRepository.findAllByCourse_IdOrderByOrderIndexAsc(courseId)
                .stream().map(this::toDto).toList();
    }

    public LessonDto update(Integer id, UpdateLessonDto dto) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Lesson lesson = getEntityById(id);
        assertOwner(lesson.getCreatedBy(), current, "Only lesson creator can edit this lesson");

        if (dto == null) {
            throw new LessonValidationException("Lesson data is required");
        }

        String title = safeTrim(dto.getTitle());
        String description = safeTrim(dto.getDescription());

        if (!StringUtils.hasText(title)) {
            throw new LessonValidationException("Lesson title must not be empty");
        }

        Integer courseId = lesson.getCourse() != null ? lesson.getCourse().getId() : null;
        if (courseId != null && !title.equalsIgnoreCase(lesson.getTitle())
                && lessonRepository.existsByCourse_IdAndTitleIgnoreCase(courseId, title)) {
            throw new DuplicateResourceException("Lesson with title '" + title + "' already exists in this course");
        }

        lesson.setTitle(title);
        lesson.setDescription(description);

        Lesson saved = lessonRepository.save(lesson);

        Integer desiredOrder = dto.getOrderIndex();
        if (desiredOrder != null && desiredOrder > 0) {
            if (courseId != null) {
                reorderWithinCourse(courseId, saved.getId(), desiredOrder);
                saved = getEntityById(saved.getId());
            }
        }

        return toDto(saved);
    }

    public LessonDto updateWithOptionalPresentation(Integer id, LessonForm form) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Lesson lesson = getEntityById(id);
        assertOwner(lesson.getCreatedBy(), current, "Only lesson creator can edit this lesson");

        if (form == null) {
            throw new LessonValidationException("Lesson data is required");
        }

        String title = safeTrim(form.getTitle());
        String description = safeTrim(form.getDescription());

        if (!StringUtils.hasText(title)) {
            throw new LessonValidationException("Lesson title must not be empty");
        }

        Integer courseId = lesson.getCourse() != null ? lesson.getCourse().getId() : null;
        if (courseId != null && !title.equalsIgnoreCase(lesson.getTitle())
                && lessonRepository.existsByCourse_IdAndTitleIgnoreCase(courseId, title)) {
            throw new DuplicateResourceException("Lesson with title '" + title + "' already exists in this course");
        }

        lesson.setTitle(title);
        lesson.setDescription(description);

        String oldUrl = lesson.getPresentationUrl();
        MultipartFile file = form.getPresentation();
        boolean hasNewFile = file != null && !file.isEmpty();

        if (hasNewFile) {
            if (courseId == null) {
                throw new LessonValidationException("Lesson course is missing");
            }
            String newUrl = presentationStorageService.uploadPresentation(courseId, file);
            lesson.setPresentationUrl(newUrl);
        }

        Lesson saved = lessonRepository.save(lesson);

        if (hasNewFile) {
            presentationStorageService.deleteByPublicUrl(oldUrl);
        }

        Integer desiredOrder = form.getOrderIndex();
        if (desiredOrder != null && desiredOrder > 0) {
            if (courseId != null) {
                reorderWithinCourse(courseId, saved.getId(), desiredOrder);
                saved = getEntityById(saved.getId());
            }
        }

        return toDto(saved);
    }

    public LessonDto replacePresentation(Integer id, MultipartFile presentation) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Lesson lesson = getEntityById(id);
        assertOwner(lesson.getCreatedBy(), current, "Only lesson creator can change lesson presentation");

        Integer courseId = lesson.getCourse() != null ? lesson.getCourse().getId() : null;
        if (courseId == null) {
            throw new LessonValidationException("Lesson course is missing");
        }

        String oldUrl = lesson.getPresentationUrl();
        String newUrl = presentationStorageService.uploadPresentation(courseId, presentation);
        lesson.setPresentationUrl(newUrl);

        Lesson saved = lessonRepository.save(lesson);
        presentationStorageService.deleteByPublicUrl(oldUrl);
        return toDto(saved);
    }

    public LessonDto deletePresentation(Integer id) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Lesson lesson = getEntityById(id);
        assertOwner(lesson.getCreatedBy(), current, "Only lesson creator can delete lesson presentation");

        String oldUrl = lesson.getPresentationUrl();
        lesson.setPresentationUrl(null);
        Lesson saved = lessonRepository.save(lesson);

        presentationStorageService.deleteByPublicUrl(oldUrl);
        return toDto(saved);
    }

    public void delete(Integer id) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Lesson lesson = getEntityById(id);
        assertOwner(lesson.getCreatedBy(), current, "Only lesson creator can delete this lesson");

        Integer courseId = lesson.getCourse() != null ? lesson.getCourse().getId() : null;
        String oldUrl = lesson.getPresentationUrl();
        lessonRepository.delete(lesson);
        if (courseId != null) {
            normalizeCourseOrder(courseId);
        }
        presentationStorageService.deleteByPublicUrl(oldUrl);
    }

    private LessonDto toDto(Lesson l) {
        LessonDto dto = new LessonDto();
        dto.setId(l.getId());
        dto.setOrderIndex(l.getOrderIndex());
        dto.setTitle(l.getTitle());
        dto.setDescription(l.getDescription());
        dto.setPresentationUrl(l.getPresentationUrl());

        if (l.getCourse() != null) {
            dto.setCourseId(l.getCourse().getId());
        }
        if (l.getCreatedBy() != null) {
            dto.setCreatedById(l.getCreatedBy().getId());
            dto.setCreatedByName(l.getCreatedBy().getName());
        }

        dto.setCreatedAt(l.getCreatedAt());
        dto.setUpdatedAt(l.getUpdatedAt());
        return dto;
    }

    private void assertOwner(User owner, User current, String message) {
        if (owner == null || owner.getId() == null || current == null || current.getId() == null
                || !owner.getId().equals(current.getId())) {
            throw new LessonAccessDeniedException(message);
        }
    }

    private void assertCanViewLesson(User current, Lesson lesson) {
        if (current == null || lesson == null || lesson.getCourse() == null) {
            throw new LessonAccessDeniedException("Forbidden");
        }

        
        if (isRole(current, ROLE_METHODIST)) {
            User owner = lesson.getCourse().getCreatedBy();
            if (owner == null || owner.getId() == null || current.getId() == null || !owner.getId().equals(current.getId())) {
                throw new LessonAccessDeniedException("Methodist can access only own courses");
            }
            return;
        }

        
        if (isRole(current, ROLE_TEACHER)) {
            Integer courseId = lesson.getCourse().getId();
            studyClassService.assertTeacherCanManageCourse(courseId, current);
            return;
        }

        if (isRole(current, ROLE_STUDENT)) {
            Integer courseId = lesson.getCourse().getId();
            classStudentService.assertStudentInCourse(
                    current.getId(),
                    courseId,
                    "Student does not belong to this course"
            );

            
            
            classOpenedLessonService.assertLessonOpenedForStudent(
                    current.getId(),
                    lesson.getId(),
                    "Lesson is not opened for your class yet"
            );
        }
    }

    private boolean isRole(User user, RoleName role) {
        return user != null
                && user.getRole() != null
                && user.getRole().getRolename() != null
                && role == user.getRole().getRolename();
    }

    
    private void reorderWithinCourse(Integer courseId, Integer lessonId, Integer desiredOrderIndex) {
        if (courseId == null || lessonId == null || desiredOrderIndex == null || desiredOrderIndex < 1) {
            return;
        }

        List<Lesson> lessons = lessonRepository.findAllByCourse_IdOrderByOrderIndexAsc(courseId);
        if (lessons.isEmpty()) {
            return;
        }

        Lesson target = null;
        for (Lesson l : lessons) {
            if (l.getId() != null && l.getId().equals(lessonId)) {
                target = l;
                break;
            }
        }
        if (target == null) {
            return;
        }

        lessons.remove(target);

        int boundedIndex = Math.min(Math.max(desiredOrderIndex, 1), lessons.size() + 1);
        lessons.add(boundedIndex - 1, target);

        applyNormalizedOrder(courseId, lessons);
    }

    private void normalizeCourseOrder(Integer courseId) {
        if (courseId == null) {
            return;
        }
        List<Lesson> lessons = lessonRepository.findAllByCourse_IdOrderByOrderIndexAsc(courseId);
        applyNormalizedOrder(courseId, lessons);
    }

    private void applyNormalizedOrder(Integer courseId, List<Lesson> lessons) {
        if (courseId == null || lessons == null || lessons.isEmpty()) {
            return;
        }

        
        final int OFFSET = 1_000_000;
        for (int i = 0; i < lessons.size(); i++) {
            Lesson l = lessons.get(i);
            l.setOrderIndex(OFFSET + (i + 1));
        }
        lessonRepository.saveAll(lessons);
        lessonRepository.flush();

        
        for (int i = 0; i < lessons.size(); i++) {
            lessons.get(i).setOrderIndex(i + 1);
        }
        lessonRepository.saveAll(lessons);
        lessonRepository.flush();
    }

    private String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
}
