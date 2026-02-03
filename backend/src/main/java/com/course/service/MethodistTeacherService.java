package com.course.service;

import com.course.entity.MethodistTeacher;
import com.course.entity.User;
import com.course.exception.ForbiddenOperationException;
import com.course.repository.MethodistTeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encapsulates access rules for METHODIST <-> TEACHER ownership.
 *
 * IMPORTANT RULE: this service uses ONLY its repository.
 * If it needs a User entity, it must be passed in from UserService.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MethodistTeacherService {

    private final MethodistTeacherRepository methodistTeacherRepository;

    @Transactional(readOnly = true)
    public boolean methodistOwnsTeacher(Integer methodistId, Integer teacherId) {
        if (methodistId == null || teacherId == null) {
            return false;
        }
        return methodistTeacherRepository.existsByMethodist_IdAndTeacher_Id(methodistId, teacherId);
    }

    public void assertMethodistOwnsTeacher(Integer methodistId, Integer teacherId, String message) {
        if (!methodistOwnsTeacher(methodistId, teacherId)) {
            throw new ForbiddenOperationException(message);
        }
    }

    public void linkTeacher(User methodist, User teacher) {
        if (methodist == null || methodist.getId() == null || teacher == null || teacher.getId() == null) {
            throw new IllegalArgumentException("Methodist and teacher are required");
        }
        if (methodistTeacherRepository.existsByMethodist_IdAndTeacher_Id(methodist.getId(), teacher.getId())) {
            return; // idempotent
        }

        MethodistTeacher link = new MethodistTeacher();
        link.setMethodist(methodist);
        link.setTeacher(teacher);
        methodistTeacherRepository.save(link);
    }

    @Transactional(readOnly = true)
    public long countOwners(Integer teacherId) {
        if (teacherId == null) {
            return 0;
        }
        return methodistTeacherRepository.countByTeacher_Id(teacherId);
    }

    public void unlinkTeacher(Integer methodistId, Integer teacherId) {
        if (methodistId == null || teacherId == null) {
            return;
        }
        methodistTeacherRepository.deleteLink(methodistId, teacherId);
    }
}
