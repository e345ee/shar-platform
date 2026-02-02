package com.course.service;

import com.course.dto.ClassJoinRequestDto;
import com.course.dto.CreateClassJoinRequestDto;
import com.course.dto.UserDto;
import com.course.entity.ClassJoinRequest;
import com.course.entity.ClassStudent;
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

    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_METHODIST = "METHODIST";

    private final ClassJoinRequestRepository joinRequestRepository;
    private final ClassStudentRepository classStudentRepository;
    private final StudyClassService classService;
    private final AuthService authService;
    private final UserService userService;

    public ClassJoinRequestDto createRequest(CreateClassJoinRequestDto dto) {
        String classCode = dto.getClassCode().trim().toUpperCase();

        StudyClass sc = classService.getEntityByJoinCode(classCode);

        if (userService.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("User with email '" + dto.getEmail() + "' already exists");
        }

        if (joinRequestRepository.existsByStudyClassIdAndEmailIgnoreCase(sc.getId(), dto.getEmail())) {
            throw new DuplicateResourceException("Join request for this class and email already exists");
        }

        ClassJoinRequest req = new ClassJoinRequest();
        req.setStudyClass(sc);
        req.setName(dto.getName());
        req.setEmail(dto.getEmail());
        req.setTgId(dto.getTgId());

        return toDto(joinRequestRepository.save(req));
    }

    @Transactional(readOnly = true)
    public List<ClassJoinRequestDto> listForClass(Integer classId) {
        StudyClass sc = classService.getEntityById(classId);
        assertCanManageRequests(sc);

        return joinRequestRepository.findAllByStudyClassIdOrderByCreatedAtDesc(classId)
                .stream().map(this::toDto).toList();
    }

    public UserDto approve(Integer classId, Integer requestId) {
        StudyClass sc = classService.getEntityById(classId);
        assertCanManageRequests(sc);

        ClassJoinRequest req = joinRequestRepository.findByIdAndStudyClassId(requestId, classId)
                .orElseThrow(() -> new ResourceNotFoundException("Join request with id " + requestId + " not found"));

        if (userService.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("User with email '" + req.getEmail() + "' already exists");
        }

        User savedStudent = userService.createStudentFromJoinRequest(req.getName(), req.getEmail(), req.getTgId());

        ClassStudent cs = new ClassStudent();
        cs.setStudyClass(sc);
        cs.setStudent(savedStudent);
        classStudentRepository.save(cs);

        joinRequestRepository.delete(req);

        return userService.toDto(savedStudent);
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

        boolean isTeacher = current.getRole() != null && ROLE_TEACHER.equalsIgnoreCase(current.getRole().getRolename());
        boolean isMethodist = current.getRole() != null && ROLE_METHODIST.equalsIgnoreCase(current.getRole().getRolename());

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

    private ClassJoinRequestDto toDto(ClassJoinRequest req) {
        ClassJoinRequestDto dto = new ClassJoinRequestDto();
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
