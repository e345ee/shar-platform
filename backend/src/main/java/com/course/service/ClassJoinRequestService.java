package com.course.service;

import com.course.dto.classroom.ClassJoinRequestByCodeRequest;
import com.course.dto.classroom.ClassJoinRequestResponse;
import com.course.dto.user.UserResponse;
import com.course.entity.ClassJoinRequest;
import com.course.entity.ClassStudent;
import com.course.entity.RoleName;
import com.course.entity.StudyClass;
import com.course.entity.User;
import com.course.exception.DuplicateResourceException;
import com.course.exception.ForbiddenOperationException;
import com.course.exception.ResourceNotFoundException;
import com.course.repository.ClassJoinRequestRepository;
import com.course.repository.ClassStudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassJoinRequestService {

    private static final RoleName ROLE_TEACHER = RoleName.TEACHER;
    private static final RoleName ROLE_METHODIST = RoleName.METHODIST;

    private final ClassJoinRequestRepository joinRequestRepository;
    private final ClassStudentRepository classStudentRepository;
    private final StudyClassService classService;
    private final AuthService authService;
    private final UserService userService;
    private final NotificationService notificationService;

    


    public ClassJoinRequestResponse createRequest(ClassJoinRequestByCodeRequest dto) {
        User current = authService.getCurrentUserEntity();
        if (current == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }
        userService.assertUserEntityHasRole(current, RoleName.STUDENT);

        String classCode = dto.getClassCode().trim().toUpperCase();
        StudyClass sc = classService.getEntityByJoinCode(classCode);

        if (classStudentRepository.existsByStudyClassIdAndStudentId(sc.getId(), current.getId())) {
            throw new DuplicateResourceException("Student is already enrolled in this class");
        }

        if (joinRequestRepository.existsByStudyClassIdAndEmailIgnoreCase(sc.getId(), current.getEmail())) {
            throw new DuplicateResourceException("Join request for this class and student already exists");
        }

        ClassJoinRequest req = new ClassJoinRequest();
        req.setStudyClass(sc);
        req.setName(current.getName());
        req.setEmail(current.getEmail());
        req.setTgId(current.getTgId());

        ClassJoinRequest saved = joinRequestRepository.save(req);

        String title = "Новая заявка на вступление";
        String msg = "Поступила заявка в класс '" + sc.getName() + "' от " + current.getName();
        if (sc.getTeacher() != null && sc.getTeacher().getId() != null) {
            notificationService.create(sc.getTeacher(),
                    com.course.entity.NotificationType.CLASS_JOIN_REQUEST,
                    title,
                    msg,
                    sc.getCourse() != null ? sc.getCourse().getId() : null,
                    sc.getId(),
                    null,
                    null,
                    null);
        }
        if (sc.getCreatedBy() != null && sc.getCreatedBy().getId() != null
                && (sc.getTeacher() == null || sc.getTeacher().getId() == null || !sc.getCreatedBy().getId().equals(sc.getTeacher().getId()))) {
            notificationService.create(sc.getCreatedBy(),
                    com.course.entity.NotificationType.CLASS_JOIN_REQUEST,
                    title,
                    msg,
                    sc.getCourse() != null ? sc.getCourse().getId() : null,
                    sc.getId(),
                    null,
                    null,
                    null);
        }

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ClassJoinRequestResponse> listForClass(Integer classId) {
        StudyClass sc = classService.getEntityById(classId);
        assertCanManageRequests(sc);

        return joinRequestRepository.findAllByStudyClassIdOrderByCreatedAtDesc(classId)
                .stream().map(this::toDto).toList();
    }

    public UserResponse approve(Integer classId, Integer requestId) {
        StudyClass sc = classService.getEntityById(classId);
        assertCanManageRequests(sc);

        ClassJoinRequest req = joinRequestRepository.findByIdAndStudyClassId(requestId, classId)
                .orElseThrow(() -> new ResourceNotFoundException("Join request with id " + requestId + " not found"));

        
        User student = userService.getUserEntityByUsernameOrEmail(req.getEmail());
        userService.assertUserEntityHasRole(student, RoleName.STUDENT);

        classStudentRepository.enrollUserToClass(student.getId(), sc.getId());
        joinRequestRepository.delete(req);

        return userService.toDto(student);
    }

    public void delete(Integer classId, Integer requestId) {
        StudyClass sc = classService.getEntityById(classId);
        assertCanManageRequests(sc);

        ClassJoinRequest req = joinRequestRepository.findByIdAndStudyClassId(requestId, classId)
                .orElseThrow(() -> new ResourceNotFoundException("Join request with id " + requestId + " not found"));

        joinRequestRepository.delete(req);
    }

    private void assertCanManageRequests(StudyClass sc) {
        User current = authService.getCurrentUserEntity();
        if (current == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }

        boolean isTeacher = current.getRole() != null && current.getRole().getRolename() == ROLE_TEACHER;
        boolean isMethodist = current.getRole() != null && current.getRole().getRolename() == ROLE_METHODIST;

        if (!isTeacher && !isMethodist) {
            throw new ForbiddenOperationException("Only TEACHER or METHODIST can manage join requests");
        }

        if (isTeacher) {
            if (sc.getTeacher() == null || sc.getTeacher().getId() == null || current.getId() == null
                    || !sc.getTeacher().getId().equals(current.getId())) {
                throw new ForbiddenOperationException("Only class teacher can manage join requests");
            }
            return;
        }

        if (sc.getCreatedBy() == null || sc.getCreatedBy().getId() == null || current.getId() == null
                || !sc.getCreatedBy().getId().equals(current.getId())) {
            throw new ForbiddenOperationException("Only class creator can manage join requests");
        }
    }

    private ClassJoinRequestResponse toDto(ClassJoinRequest req) {
        ClassJoinRequestResponse dto = new ClassJoinRequestResponse();
        dto.setId(req.getId());
        if (req.getStudyClass() != null) {
            dto.setClassId(req.getStudyClass().getId());
            dto.setClassName(req.getStudyClass().getName());
        }
        dto.setName(req.getName());
        dto.setEmail(req.getEmail());
        dto.setTgId(req.getTgId());
        dto.setCreatedAt(req.getCreatedAt());
        return dto;
    }

}
