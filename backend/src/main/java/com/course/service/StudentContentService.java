package com.course.service;

import com.course.dto.CourseDto;
import com.course.dto.LessonDto;
import com.course.entity.Course;
import com.course.entity.RoleName;
import com.course.entity.User;
import com.course.repository.ClassStudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentContentService {

    private static final RoleName ROLE_STUDENT = RoleName.STUDENT;

    private final AuthService authService;
    private final UserService userService;

    private final ClassStudentRepository classStudentRepository;
    private final LessonService lessonService;
    private final ClassOpenedLessonService classOpenedLessonService;

    public List<CourseDto> listMyCourses() {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_STUDENT);

        List<Course> courses = classStudentRepository.findDistinctCoursesByStudentId(current.getId());
        return courses.stream().map(this::toDto).toList();
    }

    public List<LessonDto> listMyLessonsInCourse(Integer courseId) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, ROLE_STUDENT);

        // LessonService checks membership; additionally, students see lessons only
        // after the responsible teacher opens them for their class.
        List<LessonDto> all = lessonService.listByCourse(courseId);
        List<Integer> openedIds = classOpenedLessonService.findOpenedLessonIdsForStudentInCourse(current.getId(), courseId);
        if (openedIds.isEmpty()) {
            return List.of();
        }
        return all.stream().filter(l -> l.getId() != null && openedIds.contains(l.getId())).toList();
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
}
