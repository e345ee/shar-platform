package com.course.controller;

import com.course.dto.StudyClassDto;
import com.course.entity.Lesson;
import com.course.entity.StudyClass;
import com.course.entity.Test;
import com.course.entity.TestStatus;
import com.course.entity.User;
import com.course.entity.RoleName;
import com.course.exception.ForbiddenOperationException;
import com.course.service.StudyClassService;
import com.course.service.AuthService;
import com.course.service.ClassOpenedLessonService;
import com.course.service.ClassOpenedTestService;
import com.course.service.LessonService;
import com.course.service.TestService;
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
    private final TestService testService;
    private final ClassOpenedTestService classOpenedTestService;

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
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<Void> openLessonForClass(@PathVariable Integer classId, @PathVariable Integer lessonId) {
        User current = authService.getCurrentUserEntity();
        RoleName role = current.getRole() != null ? current.getRole().getRolename() : null;

        StudyClass sc = classService.getEntityById(classId);
        if (role == RoleName.TEACHER) {
            if (sc.getTeacher() == null || sc.getTeacher().getId() == null || !sc.getTeacher().getId().equals(current.getId())) {
                throw new ForbiddenOperationException("Teacher can open lessons only for own classes");
            }
        } else if (role == RoleName.METHODIST) {
            if (sc.getCreatedBy() == null || sc.getCreatedBy().getId() == null || !sc.getCreatedBy().getId().equals(current.getId())) {
                throw new ForbiddenOperationException("Methodist can open lessons only for own classes");
            }
        } else {
            throw new ForbiddenOperationException("Forbidden");
        }

        Lesson lesson = lessonService.getEntityById(lessonId);
        if (lesson.getCourse() == null || sc.getCourse() == null || sc.getCourse().getId() == null
                || !sc.getCourse().getId().equals(lesson.getCourse().getId())) {
            return ResponseEntity.badRequest().build();
        }

        classOpenedLessonService.openLessonForClass(sc, lesson, current);
        return ResponseEntity.ok().build();
    }

    /**
     * Teacher opens a TEST for a specific class.
     *
     * SRS 3.2.3: "Открыть доступ к тесту".
     * Students will be able to see/start this test only after it is opened for their class.
     */
    @PostMapping("/api/teachers/me/classes/{classId}/tests/{testId}/open")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<Void> openTestForClass(@PathVariable Integer classId, @PathVariable Integer testId) {
        User current = authService.getCurrentUserEntity();
        RoleName role = current.getRole() != null ? current.getRole().getRolename() : null;

        StudyClass sc = classService.getEntityById(classId);
        if (role == RoleName.TEACHER) {
            if (sc.getTeacher() == null || sc.getTeacher().getId() == null || !sc.getTeacher().getId().equals(current.getId())) {
                throw new ForbiddenOperationException("Teacher can open tests only for own classes");
            }
        } else if (role == RoleName.METHODIST) {
            if (sc.getCreatedBy() == null || sc.getCreatedBy().getId() == null || !sc.getCreatedBy().getId().equals(current.getId())) {
                throw new ForbiddenOperationException("Methodist can open tests only for own classes");
            }
        } else {
            throw new ForbiddenOperationException("Forbidden");
        }

        Test test = testService.getEntityById(testId);
        if (test.getCourse() == null || sc.getCourse() == null || sc.getCourse().getId() == null
                || test.getCourse().getId() == null || !sc.getCourse().getId().equals(test.getCourse().getId())) {
            return ResponseEntity.badRequest().build();
        }

        // We open only READY tests, otherwise the action is meaningless for students.
        if (test.getStatus() == null || test.getStatus() != TestStatus.READY) {
            return ResponseEntity.badRequest().build();
        }

        classOpenedTestService.openTestForClass(sc, test);
        return ResponseEntity.ok().build();
    }
}
