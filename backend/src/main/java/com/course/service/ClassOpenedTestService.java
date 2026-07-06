package com.course.service;

import com.course.entity.ClassOpenedTest;
import com.course.entity.StudyClass;
import com.course.entity.Test;
import com.course.exception.ForbiddenOperationException;
import com.course.repository.ClassOpenedTestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassOpenedTestService {

    private final ClassOpenedTestRepository classOpenedTestRepository;

    @Transactional(readOnly = true)
    public boolean isTestOpenedForStudent(Integer studentId, Integer testId) {
        if (studentId == null || testId == null) {
            return false;
        }
        return classOpenedTestRepository.isTestOpenedForStudent(studentId, testId);
    }

    @Transactional(readOnly = true)
    public void assertTestOpenedForStudent(Integer studentId, Integer testId, String message) {
        if (!isTestOpenedForStudent(studentId, testId)) {
            throw new ForbiddenOperationException(message);
        }
    }

    @Transactional(readOnly = true)
    public List<Integer> findOpenedTestIdsForStudentInLesson(Integer studentId, Integer lessonId) {
        if (studentId == null || lessonId == null) {
            return List.of();
        }
        return classOpenedTestRepository.findOpenedTestIdsForStudentInLesson(studentId, lessonId);
    }

    @Transactional(readOnly = true)
    public List<Integer> findOpenedTestIdsForStudentInCourse(Integer studentId, Integer courseId) {
        if (studentId == null || courseId == null) {
            return List.of();
        }
        return classOpenedTestRepository.findOpenedTestIdsForStudentInCourse(studentId, courseId);
    }

    @Transactional
    public void openTestForClass(StudyClass studyClass, Test test) {
        if (studyClass == null || studyClass.getId() == null || test == null || test.getId() == null) {
            throw new ForbiddenOperationException("Invalid class/test");
        }
        if (classOpenedTestRepository.existsByStudyClass_IdAndTest_Id(studyClass.getId(), test.getId())) {
            return;
        }
        ClassOpenedTest rec = new ClassOpenedTest();
        rec.setStudyClass(studyClass);
        rec.setTest(test);
        classOpenedTestRepository.save(rec);
    }

    @Transactional(readOnly = true)
    public List<Integer> findOpenedClassIdsByTestId(Integer testId) {
        if (testId == null) {
            return List.of();
        }
        return classOpenedTestRepository.findOpenedClassIdsByTestId(testId);
    }
}
