package com.course.controller;

import com.course.dto.StudyClassDto;
import com.course.entity.Lesson;
import com.course.entity.StudyClass;
import com.course.entity.User;
import com.course.service.StudyClassService;
import com.course.service.AuthService;
import com.course.service.ClassOpenedLessonService;
import com.course.service.LessonService;
import com.course.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
public class TeacherClassesController {

    private final StudyClassService classService;
    private final AuthService authService;
    private final UserService userService;
    private final LessonService lessonService;
    private final ClassOpenedLessonService classOpenedLessonService;

    @GetMapping("/api/teachers/me/classes")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<List<StudyClassDto>> getMyClasses() {
        return ResponseEntity.ok(classService.getMyClasses());
    }

    @GetMapping("/api/teachers/me/classes/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<StudyClassDto> getMyClassById(@PathVariable Integer id) {
        return ResponseEntity.ok(classService.getMyClassById(id));
    }

    /**
     * Teacher opens a lesson for a specific class.
     * Until a lesson is opened, students in the class cannot view the lesson content or its test(s).
     */
    @PostMapping("/api/teachers/me/classes/{classId}/lessons/{lessonId}/open")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> openLessonForClass(@PathVariable Integer classId, @PathVariable Integer lessonId) {
        User current = authService.getCurrentUserEntity();
        userService.assertUserEntityHasRole(current, "TEACHER");

        StudyClass sc = classService.getEntityById(classId);
        if (sc.getTeacher() == null || sc.getTeacher().getId() == null || !sc.getTeacher().getId().equals(current.getId())) {
            return ResponseEntity.status(403).build();
        }

        Lesson lesson = lessonService.getEntityById(lessonId);
        if (lesson.getCourse() == null || sc.getCourse() == null || sc.getCourse().getId() == null
                || !sc.getCourse().getId().equals(lesson.getCourse().getId())) {
            return ResponseEntity.badRequest().build();
        }

        classOpenedLessonService.openLessonForClass(sc, lesson, current);
        return ResponseEntity.ok().build();
    }
}
