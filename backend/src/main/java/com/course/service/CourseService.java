package com.course.service;

import com.course.dto.course.CourseResponse;
import com.course.dto.course.CourseUpsertRequest;
import com.course.entity.Course;
import com.course.entity.RoleName;
import com.course.entity.User;
import com.course.exception.ForbiddenOperationException;
import com.course.exception.ResourceNotFoundException;
import com.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Transactional
public class CourseService {

    private static final RoleName ROLE_METHODIST = RoleName.METHODIST;

    private final CourseRepository courseRepository;
    private final AuthService authService;
    private final UserService userService;

    public CourseResponse create(@Valid @NotNull CourseUpsertRequest dto) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Course course = new Course();
        course.setName(dto.getName());
        course.setDescription(dto.getDescription());
        course.setCreatedBy(current);

        return toDto(courseRepository.save(course));
    }

    @Transactional(readOnly = true)
    public CourseResponse getById(Integer id) {
        return toDto(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public Course getEntityById(Integer id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course with id " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getAll() {
        return courseRepository.findAll().stream().map(this::toDto).toList();
    }

    public CourseResponse update(@NotNull Integer id, @Valid @NotNull CourseUpsertRequest dto) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Course course = getEntityById(id);
        assertOwner(course.getCreatedBy(), current, "Only course creator can edit this course");

        course.setName(dto.getName());
        course.setDescription(dto.getDescription());

        return toDto(courseRepository.save(course));
    }

    public void delete(@NotNull Integer id) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Course course = getEntityById(id);
        assertOwner(course.getCreatedBy(), current, "Only course creator can delete this course");

        courseRepository.delete(course);
    }

    private CourseResponse toDto(Course c) {
        CourseResponse dto = new CourseResponse();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setDescription(c.getDescription());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());

        if (c.getCreatedBy() != null) {
            dto.setCreatedById(c.getCreatedBy().getId());
            dto.setCreatedByName(c.getCreatedBy().getName());
        }
        return dto;
    }

    private void assertOwner(User owner, User current, String message) {
        if (owner == null || owner.getId() == null || current == null || current.getId() == null
                || !owner.getId().equals(current.getId())) {
            throw new ForbiddenOperationException(message);
        }
    }
}
