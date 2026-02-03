package com.course.service;

import com.course.entity.ClassOpenedLesson;
import com.course.entity.Lesson;
import com.course.entity.StudyClass;
import com.course.entity.User;
import com.course.exception.ForbiddenOperationException;
import com.course.repository.ClassOpenedLessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassOpenedLessonService {

    private final ClassOpenedLessonRepository classOpenedLessonRepository;

    @Transactional(readOnly = true)
    public boolean isLessonOpenedForStudent(Integer studentId, Integer lessonId) {
        if (studentId == null || lessonId == null) {
            return false;
        }
        return classOpenedLessonRepository.isLessonOpenedForStudent(studentId, lessonId);
    }

    @Transactional(readOnly = true)
    public void assertLessonOpenedForStudent(Integer studentId, Integer lessonId, String message) {
        if (!isLessonOpenedForStudent(studentId, lessonId)) {
            throw new ForbiddenOperationException(message);
        }
    }

    @Transactional(readOnly = true)
    public List<Integer> findOpenedLessonIdsForStudentInCourse(Integer studentId, Integer courseId) {
        if (studentId == null || courseId == null) {
            return List.of();
        }
        return classOpenedLessonRepository.findOpenedLessonIdsForStudentInCourse(studentId, courseId);
    }

    @Transactional
    public void openLessonForClass(StudyClass studyClass, Lesson lesson, User teacher) {
        if (studyClass == null || lesson == null) {
            throw new ForbiddenOperationException("Invalid class/lesson");
        }
        // teacher validation should be done by caller
        if (classOpenedLessonRepository.existsByStudyClass_IdAndLesson_Id(studyClass.getId(), lesson.getId())) {
            return;
        }
        ClassOpenedLesson rec = new ClassOpenedLesson();
        rec.setStudyClass(studyClass);
        rec.setLesson(lesson);
        classOpenedLessonRepository.save(rec);
    }
}
