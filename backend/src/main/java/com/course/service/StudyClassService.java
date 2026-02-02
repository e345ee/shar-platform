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

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudyClassService {

    private static final String ROLE_METHODIST = "METHODIST";
    private static final String ROLE_TEACHER = "TEACHER";

    private final StudyClassRepository classRepository;

    private final CourseService courseService;
    private final UserService userService;
    private final AuthService authService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] JOIN_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

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
        sc.setJoinCode(generateUniqueJoinCode());

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
    public StudyClass getEntityByJoinCode(String joinCode) {
        return classRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new ResourceNotFoundException("Class with code " + joinCode + " not found"));
    }

    @Transactional(readOnly = true)
    public List<StudyClassDto> getAllByCourse(Integer courseId) {
        // optional: validate that course exists using CourseService
        courseService.getEntityById(courseId);
        return classRepository.findAllByCourseId(courseId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<StudyClassDto> getMyClasses() {
        User current = authService.getCurrentUserEntity();
        String role = current.getRole() != null ? current.getRole().getRolename() : null;

        if (role == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }

        if (ROLE_TEACHER.equalsIgnoreCase(role)) {
            return classRepository.findAllByTeacherId(current.getId()).stream().map(this::toDto).toList();
        }

        if (ROLE_METHODIST.equalsIgnoreCase(role)) {
            return classRepository.findAllByCreatedById(current.getId()).stream().map(this::toDto).toList();
        }

        throw new ForbiddenOperationException("User must have role TEACHER or METHODIST");
    }


    @Transactional(readOnly = true)
    public StudyClassDto getMyClassById(Integer id) {
        User current = authService.getCurrentUserEntity();
        String role = current.getRole() != null ? current.getRole().getRolename() : null;

        if (role == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }

        StudyClass sc;
        if (ROLE_TEACHER.equalsIgnoreCase(role)) {
            sc = classRepository.findByIdAndTeacherId(id, current.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class with id " + id + " not found"));
            return toDto(sc);
        }

        if (ROLE_METHODIST.equalsIgnoreCase(role)) {
            sc = classRepository.findByIdAndCreatedById(id, current.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class with id " + id + " not found"));
            return toDto(sc);
        }

        throw new ForbiddenOperationException("User must have role TEACHER or METHODIST");
    }

    @Transactional(readOnly = true)
    public void assertTeacherCanManageCourse(Integer courseId, User teacher) {
        if (teacher == null || teacher.getId() == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }

        userService.assertUserEntityHasRole(teacher, ROLE_TEACHER);

        boolean ok = classRepository.findAllByTeacherId(teacher.getId()).stream()
                .anyMatch(c -> c.getCourse() != null && c.getCourse().getId() != null && c.getCourse().getId().equals(courseId));
        if (!ok) {
            throw new ForbiddenOperationException("Teacher can manage only own courses");
        }
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

        dto.setJoinCode(sc.getJoinCode());

        if (sc.getCourse() != null) dto.setCourseId(sc.getCourse().getId());

        if (sc.getTeacher() != null) {
            dto.setTeacherId(sc.getTeacher().getId());
            dto.setTeacherName(sc.getTeacher().getName());
        }

        if (sc.getCreatedBy() != null) {
            dto.setCreatedById(sc.getCreatedBy().getId());
            dto.setCreatedByName(sc.getCreatedBy().getName());
        }

        dto.setJoinCode(sc.getJoinCode());

        dto.setCreatedAt(sc.getCreatedAt());
        dto.setUpdatedAt(sc.getUpdatedAt());
        return dto;
    }

    private String generateUniqueJoinCode() {
        // 8 chars, no 0/1/I/O to reduce confusion when typing
        for (int attempt = 0; attempt < 20; attempt++) {
            char[] buf = new char[8];
            for (int i = 0; i < buf.length; i++) {
                buf[i] = JOIN_CODE_ALPHABET[SECURE_RANDOM.nextInt(JOIN_CODE_ALPHABET.length)];
            }
            String code = new String(buf);
            if (!classRepository.existsByJoinCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate unique class code");
    }

    private void assertOwner(User owner, User current, String message) {
        if (owner == null || owner.getId() == null || current == null || current.getId() == null
                || !owner.getId().equals(current.getId())) {
            throw new ForbiddenOperationException(message);
        }
    }
}
