package com.course.service;

import com.course.exception.ForbiddenOperationException;
import com.course.repository.ClassStudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassStudentService {

    private final ClassStudentRepository classStudentRepository;

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
}
