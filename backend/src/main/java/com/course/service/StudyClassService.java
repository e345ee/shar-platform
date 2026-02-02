package com.course.service;

import com.course.dto.StudyClassDto;
import com.course.entity.Course;
import com.course.entity.StudyClass;
import com.course.entity.User;
import com.course.exception.ForbiddenOperationException;
import com.course.exception.ResourceNotFoundException;
import com.course.repository.StudyClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudyClassService {

    private static final String ROLE_METHODIST = "METHODIST";
    private static final String ROLE_TEACHER = "TEACHER";

    private final StudyClassRepository classRepository;

    // "чужие" сущности берём через сервисы, а не репозитории
    private final CourseService courseService;
    private final UserService userService;
    private final AuthService authService;

    public StudyClassDto create(StudyClassDto dto) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Course course = courseService.getEntityById(dto.getCourseId());

        User teacher = null;
        if (dto.getTeacherId() != null) {
            teacher = userService.getUserEntityById(dto.getTeacherId());
            userService.assertUserEntityHasRole(teacher, ROLE_TEACHER);
        }

        StudyClass sc = new StudyClass();
        sc.setName(dto.getName());
        sc.setCourse(course);
        sc.setTeacher(teacher);
        sc.setCreatedBy(current);

        return toDto(classRepository.save(sc));
    }

    @Transactional(readOnly = true)
    public StudyClassDto getById(Integer id) {
        return toDto(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public StudyClass getEntityById(Integer id) {
        return classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class with id " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<StudyClassDto> getAllByCourse(Integer courseId) {
        // optional: validate that course exists using CourseService
        courseService.getEntityById(courseId);
        return classRepository.findAllByCourseId(courseId).stream().map(this::toDto).toList();
    }

    public StudyClassDto update(Integer id, StudyClassDto dto) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        StudyClass sc = getEntityById(id);

        // По аналогии с курсом: редактирует только создатель класса
        assertOwner(sc.getCreatedBy(), current, "Only class creator can edit this class");

        sc.setName(dto.getName());

        // курс НЕ меняем (упрощение, чтобы не ломать логику)
        if (dto.getCourseId() != null && !dto.getCourseId().equals(sc.getCourse().getId())) {
            throw new ForbiddenOperationException("Changing courseId is not allowed");
        }

        if (dto.getTeacherId() == null) {
            sc.setTeacher(null);
        } else {
            User teacher = userService.getUserEntityById(dto.getTeacherId());
            userService.assertUserEntityHasRole(teacher, ROLE_TEACHER);
            sc.setTeacher(teacher);
        }

        return toDto(classRepository.save(sc));
    }

    public void delete(Integer id) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        StudyClass sc = getEntityById(id);
        assertOwner(sc.getCreatedBy(), current, "Only class creator can delete this class");

        classRepository.delete(sc);
    }

    private StudyClassDto toDto(StudyClass sc) {
        StudyClassDto dto = new StudyClassDto();
        dto.setId(sc.getId());
        dto.setName(sc.getName());

        if (sc.getCourse() != null) dto.setCourseId(sc.getCourse().getId());

        if (sc.getTeacher() != null) {
            dto.setTeacherId(sc.getTeacher().getId());
            dto.setTeacherName(sc.getTeacher().getName());
        }

        if (sc.getCreatedBy() != null) {
            dto.setCreatedById(sc.getCreatedBy().getId());
            dto.setCreatedByName(sc.getCreatedBy().getName());
        }

        dto.setCreatedAt(sc.getCreatedAt());
        dto.setUpdatedAt(sc.getUpdatedAt());
        return dto;
    }

    private void assertOwner(User owner, User current, String message) {
        if (owner == null || owner.getId() == null || current == null || current.getId() == null
                || !owner.getId().equals(current.getId())) {
            throw new ForbiddenOperationException(message);
        }
    }
}
