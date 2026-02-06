package com.course.service;

import com.course.entity.ClassStudent;
import com.course.entity.RoleName;
import com.course.entity.StudyClass;
import com.course.entity.User;
import com.course.exception.ClassStudentAccessDeniedException;
import com.course.exception.ForbiddenOperationException;
import com.course.exception.ResourceNotFoundException;
import com.course.exception.StudentNotEnrolledInClassException;
import com.course.repository.ClassStudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassStudentService {

    private final ClassStudentRepository classStudentRepository;

    private final StudyClassService classService;
    private final AuthService authService;
    private final UserService userService;

    private static final RoleName ROLE_TEACHER = RoleName.TEACHER;
    private static final RoleName ROLE_METHODIST = RoleName.METHODIST;
    private static final RoleName ROLE_STUDENT = RoleName.STUDENT;

    public boolean existsStudentInClass(Integer studentId, Integer classId) {
        if (studentId == null || classId == null) {
            return false;
        }
        return classStudentRepository.existsByStudyClassIdAndStudentId(classId, studentId);
    }

    public void assertStudentInClass(Integer studentId, Integer classId, String message) {
        if (!existsStudentInClass(studentId, classId)) {
            throw new ForbiddenOperationException(message);
        }
    }

    public java.util.List<Integer> findClassIdsByStudentInCourse(Integer studentId, Integer courseId) {
        if (studentId == null || courseId == null) {
            return java.util.List.of();
        }
        return classStudentRepository.findClassIdsByStudentInCourse(studentId, courseId);
    }

    public java.util.List<Integer> findDistinctStudentIdsByCourseId(Integer courseId) {
        if (courseId == null) {
            return java.util.List.of();
        }
        return classStudentRepository.findDistinctStudentIdsByCourseId(courseId);
    }

    public java.util.List<Integer> findDistinctTeacherIdsByStudentInCourse(Integer studentId, Integer courseId) {
        if (studentId == null || courseId == null) {
            return java.util.List.of();
        }
        return classStudentRepository.findDistinctTeacherIdsByStudentInCourse(studentId, courseId);
    }

    public boolean existsStudentInCourse(Integer studentId, Integer courseId) {
        if (studentId == null || courseId == null) {
            return false;
        }
        return classStudentRepository.existsStudentInCourse(studentId, courseId);
    }

    public void assertStudentInCourse(Integer studentId, Integer courseId, String message) {
        if (!existsStudentInCourse(studentId, courseId)) {
            throw new ForbiddenOperationException(message);
        }
    }

    public boolean existsStudentInTeacherClasses(Integer studentId, Integer teacherId) {
        if (studentId == null || teacherId == null) {
            return false;
        }
        return classStudentRepository.existsStudentInTeacherClasses(studentId, teacherId);
    }

    public void assertStudentInTeacherClasses(Integer studentId, Integer teacherId, String message) {
        if (!existsStudentInTeacherClasses(studentId, teacherId)) {
            throw new ForbiddenOperationException(message);
        }
    }

    public boolean existsStudentInTeacherCourse(Integer studentId, Integer teacherId, Integer courseId) {
        if (studentId == null || teacherId == null || courseId == null) {
            return false;
        }
        return classStudentRepository.existsStudentInTeacherCourse(studentId, teacherId, courseId);
    }

    public void assertStudentInTeacherCourse(Integer studentId, Integer teacherId, Integer courseId, String message) {
        if (!existsStudentInTeacherCourse(studentId, teacherId, courseId)) {
            throw new ForbiddenOperationException(message);
        }
    }

    public boolean existsStudentInMethodistCourses(Integer studentId, Integer methodistId) {
        if (studentId == null || methodistId == null) {
            return false;
        }
        return classStudentRepository.existsStudentInMethodistCourses(studentId, methodistId);
    }

    public void assertStudentInMethodistCourses(Integer studentId, Integer methodistId, String message) {
        if (!existsStudentInMethodistCourses(studentId, methodistId)) {
            throw new ForbiddenOperationException(message);
        }
    }

    /**
     * Business operation (SRS 3.2.1): remove a student from a specific study class.
     *
     * Access rules:
     * - TEACHER: only for own classes (class.teacher == current user)
     * - METHODIST: only for classes created by current user (class.createdBy == current user)
     */
    @Transactional
    public void removeStudentFromClass(Integer classId, Integer studentId) {
        if (classId == null || studentId == null) {
            throw new IllegalArgumentException("classId and studentId are required");
        }

        User current = authService.getCurrentUserEntity();
        if (current == null || current.getRole() == null || current.getRole().getRolename() == null) {
            // keep consistent with other services in the project
            throw new ForbiddenOperationException("Unauthenticated");
        }

        RoleName role = current.getRole().getRolename();
        boolean isTeacher = role == ROLE_TEACHER;
        boolean isMethodist = role == ROLE_METHODIST;
        if (!isTeacher && !isMethodist) {
            throw new ClassStudentAccessDeniedException("Only TEACHER or METHODIST can remove students from classes");
        }

        StudyClass sc = classService.getEntityById(classId);

        if (isTeacher) {
            if (sc.getTeacher() == null || sc.getTeacher().getId() == null || current.getId() == null
                    || !sc.getTeacher().getId().equals(current.getId())) {
                throw new ClassStudentAccessDeniedException("Only class teacher can remove students");
            }
        }

        if (isMethodist) {
            if (sc.getCreatedBy() == null || sc.getCreatedBy().getId() == null || current.getId() == null
                    || !sc.getCreatedBy().getId().equals(current.getId())) {
                throw new ClassStudentAccessDeniedException("Only class creator can remove students");
            }
        }

        User student = userService.getUserEntityById(studentId);
        userService.assertUserEntityHasRole(student, ROLE_STUDENT);

        ClassStudent cs = classStudentRepository.findByStudyClassIdAndStudentId(classId, studentId)
                .orElseThrow(() -> new StudentNotEnrolledInClassException(
                        "Student with id " + studentId + " is not enrolled in class " + classId));

        classStudentRepository.delete(cs);
    }
}
