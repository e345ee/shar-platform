package com.course.service;

import com.course.dto.ClassJoinRequestDto;
import com.course.dto.CreateClassJoinRequestDto;
import com.course.dto.UserDto;
import com.course.entity.ClassJoinRequest;
import com.course.entity.ClassStudent;
import com.course.entity.Role;
import com.course.entity.StudyClass;
import com.course.entity.User;
import com.course.exception.DuplicateResourceException;
import com.course.exception.ForbiddenOperationException;
import com.course.exception.ResourceNotFoundException;
import com.course.repository.ClassJoinRequestRepository;
import com.course.repository.ClassStudentRepository;
import com.course.repository.RoleRepository;
import com.course.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassJoinRequestService {

    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_METHODIST = "METHODIST";
    private static final String ROLE_STUDENT = "STUDENT";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%".toCharArray();

    private final ClassJoinRequestRepository joinRequestRepository;
    private final ClassStudentRepository classStudentRepository;
    private final StudyClassService classService;
    private final AuthService authService;
    private final UserService userService;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Public endpoint: a user from another service submits a join request by 8-char class code.
     */
    public ClassJoinRequestDto createRequest(CreateClassJoinRequestDto dto) {
        String classCode = dto.getClassCode().trim().toUpperCase();

        StudyClass sc = classService.getEntityByJoinCode(classCode);

        // If user already exists in the system by email -> reject early.
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("User with email '" + dto.getEmail() + "' already exists");
        }

        // Prevent duplicate requests to the same class.
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

    /**
     * TEACHER responsible for the class (or METHODIST creator) approves the request:
     * - creates STUDENT user
     * - attaches to class
     * - deletes the join request
     */
    public UserDto approve(Integer classId, Integer requestId) {
        StudyClass sc = classService.getEntityById(classId);
        assertCanManageRequests(sc);

        ClassJoinRequest req = joinRequestRepository.findByIdAndStudyClassId(requestId, classId)
                .orElseThrow(() -> new ResourceNotFoundException("Join request with id " + requestId + " not found"));

        // Create student
        Role studentRole = roleRepository.findByRolename(ROLE_STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Role '" + ROLE_STUDENT + "' not found"));

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("User with email '" + req.getEmail() + "' already exists");
        }

        // name is unique in DB -> if collision happens, we append small suffix
        String name = req.getName();
        if (userRepository.existsByName(name)) {
            name = makeUniqueName(name);
        }

        User student = new User();
        student.setRole(studentRole);
        student.setName(name);
        student.setEmail(req.getEmail());
        student.setTgId(req.getTgId());
        student.setPassword(passwordEncoder.encode(generateTemporaryPassword(12)));
        User savedStudent = userRepository.save(student);

        // Attach student to class
        ClassStudent cs = new ClassStudent();
        cs.setStudyClass(sc);
        cs.setStudent(savedStudent);
        classStudentRepository.save(cs);

        // Remove request
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

        // TEACHER: must be assigned as responsible teacher for the class
        if (isTeacher) {
            if (sc.getTeacher() == null || sc.getTeacher().getId() == null || current.getId() == null
                    || !sc.getTeacher().getId().equals(current.getId())) {
                throw new ForbiddenOperationException("Only class teacher can manage join requests");
            }
            return;
        }

        // METHODIST: inherits teacher behavior but within the course
        // In our simplified model we consider METHODIST to have rights over classes they created.
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

    private String generateTemporaryPassword(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(PASSWORD_ALPHABET[SECURE_RANDOM.nextInt(PASSWORD_ALPHABET.length)]);
        }
        return sb.toString();
    }

    private String makeUniqueName(String base) {
        for (int i = 1; i <= 50; i++) {
            String candidate = base + "_" + i;
            if (!userRepository.existsByName(candidate)) {
                return candidate;
            }
        }
        throw new DuplicateResourceException("Unable to generate unique user name");
    }
}
