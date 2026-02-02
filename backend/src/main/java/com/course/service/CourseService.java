package com.course.service;

import com.course.dto.CourseDto;
import com.course.entity.Course;
import com.course.entity.User;
import com.course.exception.ForbiddenOperationException;
import com.course.exception.ResourceNotFoundException;
import com.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {

    private static final String ROLE_METHODIST = "METHODIST";

    private final CourseRepository courseRepository;
    private final AuthService authService;
    private final UserService userService;

    public CourseDto create(CourseDto dto) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Course course = new Course();
        course.setName(dto.getName());
        course.setDescription(dto.getDescription());
        course.setCreatedBy(current);

        return toDto(courseRepository.save(course));
    }

    @Transactional(readOnly = true)
    public CourseDto getById(Integer id) {
        return toDto(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public Course getEntityById(Integer id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course with id " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<CourseDto> getAll() {
        return courseRepository.findAll().stream().map(this::toDto).toList();
    }

    public CourseDto update(Integer id, CourseDto dto) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Course course = getEntityById(id);
        assertOwner(course.getCreatedBy(), current, "Only course creator can edit this course");

        course.setName(dto.getName());
        course.setDescription(dto.getDescription());

        return toDto(courseRepository.save(course));
    }

    public void delete(Integer id) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_METHODIST);

        Course course = getEntityById(id);
        assertOwner(course.getCreatedBy(), current, "Only course creator can delete this course");

        courseRepository.delete(course);
    }

    private CourseDto toDto(Course c) {
        CourseDto dto = new CourseDto();
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
