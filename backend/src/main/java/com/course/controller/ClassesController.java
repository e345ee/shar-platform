package com.course.controller;

import com.course.dto.achievement.StudentAchievementResponse;
import com.course.dto.common.PageResponse;
import com.course.dto.user.UserResponse;
import com.course.dto.classroom.StudyClassResponse;
import com.course.dto.classroom.StudyClassUpsertRequest;
import com.course.entity.Lesson;
import com.course.entity.RoleName;
import com.course.entity.StudyClass;
import com.course.entity.Test;
import com.course.entity.TestStatus;
import com.course.entity.User;
import com.course.exception.ForbiddenOperationException;
import com.course.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClassesController {

    private final StudyClassService classService;
    private final AuthService authService;
    private final LessonService lessonService;
    private final ClassOpenedLessonService classOpenedLessonService;
    private final TestService testService;
    private final ClassOpenedTestService classOpenedTestService;
    private final ClassStudentService classStudentService;
    private final ClassAchievementFeedService feedService;
    private final UserService userService;

    

    @PostMapping("/classes")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<StudyClassResponse> create(@Valid @RequestBody StudyClassUpsertRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classService.create(dto));
    }

    @GetMapping("/classes/{id}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<StudyClassResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(classService.getById(id));
    }

    @GetMapping("/courses/{courseId}/classes")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<List<StudyClassResponse>> getByCourse(@PathVariable Integer courseId) {
        return ResponseEntity.ok(classService.getAllByCourse(courseId));
    }

    @PutMapping("/classes/{id}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<StudyClassResponse> update(@PathVariable Integer id, @Valid @RequestBody StudyClassUpsertRequest dto) {
        return ResponseEntity.ok(classService.update(id, dto));
    }

    @DeleteMapping("/classes/{id}")
    @PreAuthorize("hasRole('METHODIST')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        classService.delete(id);
        return ResponseEntity.noContent().build();
    }

    

    @GetMapping("/classes/my")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<List<StudyClassResponse>> getMyClasses() {
        return ResponseEntity.ok(classService.getMyClasses());
    }

    @GetMapping("/classes/my/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<StudyClassResponse> getMyClassById(@PathVariable Integer id) {
        return ResponseEntity.ok(classService.getMyClassById(id));
    }

    

    @PostMapping("/classes/{classId}/lessons/{lessonId}/open")
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

    @PostMapping("/classes/{classId}/activities/{activityId}/open")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<Void> openActivityForClass(@PathVariable Integer classId, @PathVariable Integer activityId) {
        User current = authService.getCurrentUserEntity();
        RoleName role = current.getRole() != null ? current.getRole().getRolename() : null;

        StudyClass sc = classService.getEntityById(classId);
        if (role == RoleName.TEACHER) {
            if (sc.getTeacher() == null || sc.getTeacher().getId() == null || !sc.getTeacher().getId().equals(current.getId())) {
                throw new ForbiddenOperationException("Teacher can open activities only for own classes");
            }
        } else if (role == RoleName.METHODIST) {
            if (sc.getCreatedBy() == null || sc.getCreatedBy().getId() == null || !sc.getCreatedBy().getId().equals(current.getId())) {
                throw new ForbiddenOperationException("Methodist can open activities only for own classes");
            }
        } else {
            throw new ForbiddenOperationException("Forbidden");
        }

        Test test = testService.getEntityById(activityId);
        if (test.getCourse() == null || sc.getCourse() == null || sc.getCourse().getId() == null
                || test.getCourse().getId() == null || !sc.getCourse().getId().equals(test.getCourse().getId())) {
            return ResponseEntity.badRequest().build();
        }

        if (test.getStatus() == null || test.getStatus() != TestStatus.READY) {
            return ResponseEntity.badRequest().build();
        }

        classOpenedTestService.openTestForClass(sc, test);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/classes/{classId}/students/{studentId}/close-course")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<Void> closeCourseForStudent(@PathVariable Integer classId, @PathVariable Integer studentId) {
        classStudentService.closeCourseForStudent(classId, studentId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/classes/{classId}/students/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER','METHODIST')")
    public ResponseEntity<Void> removeStudentFromClass(@PathVariable Integer classId, @PathVariable Integer studentId) {
        classStudentService.removeStudentFromClass(classId, studentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/classes/{classId}/students")
    @PreAuthorize("hasAnyRole('ADMIN','METHODIST','TEACHER','STUDENT')")
    public ResponseEntity<PageResponse<UserResponse>> listClassStudents(@PathVariable Integer classId, Pageable pageable) {
        return ResponseEntity.ok(classStudentService.listStudentsInClass(classId, pageable));
    }

    

    @GetMapping("/classes/{classId}/achievement-feed")
    @PreAuthorize("hasAnyRole('ADMIN','METHODIST','TEACHER','STUDENT')")
    public ResponseEntity<PageResponse<StudentAchievementResponse>> getClassAchievementFeed(@PathVariable Integer classId, Pageable pageable) {
        User current = authService.getCurrentUserEntity();
        RoleName role = current != null && current.getRole() != null ? current.getRole().getRolename() : null;
        if (role == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }

        if (RoleName.STUDENT == role) {
            userService.assertUserEntityHasRole(current, RoleName.STUDENT);
            classStudentService.assertStudentInClass(current.getId(), classId, "Student can view feed only for own classes");
        } else if (RoleName.TEACHER == role || RoleName.METHODIST == role) {
            classService.getMyClassById(classId);
        }

        return ResponseEntity.ok(feedService.getFeedForClass(classId, pageable));
    }
}
