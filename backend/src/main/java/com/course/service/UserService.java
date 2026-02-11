package com.course.service;

import com.course.dto.common.PageResponse;
import com.course.dto.user.ProfileUpdateRequest;
import com.course.dto.user.UserResponse;
import com.course.dto.user.UserUpsertRequest;
import com.course.entity.Role;
import com.course.entity.RoleName;
import com.course.entity.User;
import com.course.exception.DuplicateResourceException;
import com.course.exception.ForbiddenOperationException;
import com.course.exception.ResourceNotFoundException;
import com.course.exception.TeacherDeletionConflictException;
import com.course.repository.RoleRepository;
import com.course.repository.StudyClassRepository;
import com.course.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Validated
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorageService avatarStorageService;
    private final MethodistTeacherService methodistTeacherService;
    private final StudyClassRepository studyClassRepository;

    private static final RoleName ROLE_ADMIN = RoleName.ADMIN;
    private static final RoleName ROLE_METHODIST = RoleName.METHODIST;
    private static final RoleName ROLE_TEACHER = RoleName.TEACHER;
    private static final RoleName ROLE_STUDENT = RoleName.STUDENT;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] TEMP_PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%".toCharArray();

    
    public UserResponse updateOwnProfile(@NotNull User currentUser, @Valid @NotNull ProfileUpdateRequest dto) {
        if (currentUser == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }

        
        boolean isAdmin = currentUser.getRole() != null
                && currentUser.getRole().getRolename() == ROLE_ADMIN;
        boolean isTeacher = currentUser.getRole() != null
                && currentUser.getRole().getRolename() == ROLE_TEACHER;
        boolean isMethodist = currentUser.getRole() != null
                && currentUser.getRole().getRolename() == ROLE_METHODIST;
        boolean isStudent = currentUser.getRole() != null
                && currentUser.getRole().getRolename() == ROLE_STUDENT;
        if (!isAdmin && !isTeacher && !isMethodist && !isStudent) {
            throw new ForbiddenOperationException("Only ADMIN, TEACHER, METHODIST or STUDENT can update profile");
        }

        
        if (dto.getName() != null) {
            String newName = dto.getName().trim();
            if (newName.isBlank()) {
                throw new IllegalArgumentException("Name cannot be blank");
            }
            if (!newName.equals(currentUser.getName()) && userRepository.existsByName(newName)) {
                throw new DuplicateResourceException("User with name '" + newName + "' already exists");
            }
            currentUser.setName(newName);
        }

        
        if (dto.getBio() != null) {
            currentUser.setBio(dto.getBio());
        }
        

        
        if (dto.getPassword() != null) {
            throw new IllegalArgumentException("Password cannot be changed via profile update. Use /api/users/me/password");
        }

        User saved = userRepository.save(currentUser);
        return convertToDto(saved);
    }

    
    public void changeOwnPassword(User currentUser, String currentPassword, String newPassword) {
        if (currentUser == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }

        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Current password cannot be blank");
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password cannot be blank");
        }
        if (newPassword.length() > 127) {
            throw new IllegalArgumentException("Password must be between 1 and 127 characters");
        }

        
        if (currentUser.getPassword() == null || !passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
            throw new ForbiddenOperationException("Current password is incorrect");
        }

        currentUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(currentUser);
    }

    
    public UserResponse uploadOwnAvatar(User currentUser, org.springframework.web.multipart.MultipartFile file) {
        if (currentUser == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }

        boolean isAdmin = currentUser.getRole() != null
                && currentUser.getRole().getRolename() == ROLE_ADMIN;
        boolean isTeacher = currentUser.getRole() != null
                && currentUser.getRole().getRolename() == ROLE_TEACHER;
        boolean isMethodist = currentUser.getRole() != null
                && currentUser.getRole().getRolename() == ROLE_METHODIST;
        boolean isStudent = currentUser.getRole() != null
                && currentUser.getRole().getRolename() == ROLE_STUDENT;
        if (!isAdmin && !isTeacher && !isMethodist && !isStudent) {
            throw new ForbiddenOperationException("Only ADMIN, TEACHER, METHODIST or STUDENT can upload avatar");
        }

        
        avatarStorageService.deleteByPublicUrl(currentUser.getPhoto());

        String publicUrl = avatarStorageService.uploadAvatar(currentUser.getId(), file);
        currentUser.setPhoto(publicUrl);
        return convertToDto(userRepository.save(currentUser));
    }

    
    public UserResponse deleteOwnAvatar(User currentUser) {
        if (currentUser == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }

        boolean isAdmin = currentUser.getRole() != null
                && currentUser.getRole().getRolename() == ROLE_ADMIN;
        boolean isTeacher = currentUser.getRole() != null
                && currentUser.getRole().getRolename() == ROLE_TEACHER;
        boolean isMethodist = currentUser.getRole() != null
                && currentUser.getRole().getRolename() == ROLE_METHODIST;
        boolean isStudent = currentUser.getRole() != null
                && currentUser.getRole().getRolename() == ROLE_STUDENT;
        if (!isAdmin && !isTeacher && !isMethodist && !isStudent) {
            throw new ForbiddenOperationException("Only ADMIN, TEACHER, METHODIST or STUDENT can delete avatar");
        }

        avatarStorageService.deleteByPublicUrl(currentUser.getPhoto());
        currentUser.setPhoto(null);
        return convertToDto(userRepository.save(currentUser));
    }

    
    public UserResponse createTeacherByMethodist(@NotNull Integer metodistUserId, @Valid @NotNull UserUpsertRequest dto) {
        assertUserHasRole(metodistUserId, ROLE_METHODIST);

        User methodist = userRepository.findById(metodistUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + metodistUserId + " not found"));

        validateUserCreateCommon(dto);

        Role teacherRole = roleRepository.findByRolename(ROLE_TEACHER)
                .orElseThrow(() -> new ResourceNotFoundException("Role '" + ROLE_TEACHER + "' not found"));

        User teacher = new User();
        teacher.setRole(teacherRole);
        teacher.setName(dto.getName());
        teacher.setEmail(dto.getEmail());
        teacher.setPassword(passwordEncoder.encode(dto.getPassword()));
        teacher.setBio(dto.getBio());
        teacher.setPhoto(dto.getPhoto());
        teacher.setTgId(dto.getTgId());

        User saved = userRepository.save(teacher);
        
        methodistTeacherService.linkTeacher(methodist, saved);
        return convertToDto(saved);
    }

    
    public void deleteTeacherByMethodist(Integer metodistUserId, Integer teacherUserId) {
        assertUserHasRole(metodistUserId, ROLE_METHODIST);

        
        methodistTeacherService.assertMethodistOwnsTeacher(
                metodistUserId,
                teacherUserId,
                "Methodist can manage only own teachers"
        );

        User teacher = userRepository.findById(teacherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + teacherUserId + " not found"));

        if (teacher.getRole() == null || teacher.getRole().getRolename() == null
                || teacher.getRole().getRolename() != ROLE_TEACHER) {
            throw new ForbiddenOperationException("Methodist can delete only TEACHER users");
        }

        
        long owners = methodistTeacherService.countOwners(teacherUserId);
        if (owners > 1) {
            throw new ForbiddenOperationException("Teacher is linked to another methodist");
        }

        
        
        var assignedClasses = studyClassRepository.findAllByTeacherId(teacherUserId);
        if (assignedClasses != null && !assignedClasses.isEmpty()) {
            String classList = assignedClasses.stream()
                    .map(c -> {
                        String id = c.getId() == null ? "?" : String.valueOf(c.getId());
                        String name = c.getName() == null ? "" : c.getName();
                        return name.isBlank() ? id : (id + ":" + name);
                    })
                    .collect(Collectors.joining(", "));
            throw new TeacherDeletionConflictException(
                    "Teacher is assigned to active classes. Reassign teacher for these classes before deletion: " + classList
            );
        }

        methodistTeacherService.unlinkTeacher(metodistUserId, teacherUserId);
        userRepository.delete(teacher);
    }

    
    public UserResponse createMethodist(@Valid @NotNull UserUpsertRequest dto) {
        validateUserCreateCommon(dto);

        Role methodistRole = roleRepository.findByRolename(ROLE_METHODIST)
                .orElseThrow(() -> new ResourceNotFoundException("Role '" + ROLE_METHODIST + "' not found"));

        User methodist = new User();
        methodist.setRole(methodistRole);
        methodist.setName(dto.getName());
        methodist.setEmail(dto.getEmail());
        methodist.setPassword(passwordEncoder.encode(dto.getPassword()));
        methodist.setBio(dto.getBio());
        methodist.setPhoto(dto.getPhoto());
        methodist.setTgId(dto.getTgId());

        User saved = userRepository.save(methodist);
        return convertToDto(saved);
    }

    
    public void deleteMethodist(Integer methodistUserId) {
        User methodist = userRepository.findById(methodistUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + methodistUserId + " not found"));

        if (methodist.getRole() == null || methodist.getRole().getRolename() == null
                || methodist.getRole().getRolename() != ROLE_METHODIST) {
            throw new ForbiddenOperationException("Admin can delete only METHODIST users via this endpoint");
        }

        userRepository.delete(methodist);
    }

    
    public void changeAdminPassword(String usernameOrEmail, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }
        if (newPassword.length() > 127) {
            throw new IllegalArgumentException("Password must be between 1 and 127 characters");
        }

        User admin = userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByName(usernameOrEmail))
                .orElseThrow(() -> new ResourceNotFoundException("User '" + usernameOrEmail + "' not found"));

        if (admin.getRole() == null || admin.getRole().getRolename() == null
                || admin.getRole().getRolename() != ROLE_ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can change admin password");
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(admin);
    }

    private void assertUserHasRole(Integer userId, RoleName requiredRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));

        if (user.getRole() == null || user.getRole().getRolename() == null
                || requiredRole != user.getRole().getRolename()) {
            throw new ForbiddenOperationException("User with id " + userId + " must have role " + requiredRole.name());
        }
    }

    
    public UserResponse toDto(User user) {
        return convertToDto(user);
    }

    
    public UserResponse createUser(@Valid @NotNull UserUpsertRequest dto) {
        if (dto.getRoleId() == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }

        validateUserCreateCommon(dto);

        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role with id " + dto.getRoleId() + " not found"));

        User user = new User();
        user.setRole(role);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setBio(dto.getBio());
        user.setPhoto(dto.getPhoto());
        user.setTgId(dto.getTgId());

        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }

    
    public UserResponse createStudent(@Valid @NotNull UserUpsertRequest dto) {
        validateUserCreateCommon(dto);

        Role studentRole = roleRepository.findByRolename(ROLE_STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Role '" + ROLE_STUDENT + "' not found"));

        User student = new User();
        student.setRole(studentRole);
        student.setName(dto.getName());
        student.setEmail(dto.getEmail());
        student.setPassword(passwordEncoder.encode(dto.getPassword()));
        student.setBio(dto.getBio());
        student.setPhoto(dto.getPhoto());
        student.setTgId(dto.getTgId());

        return convertToDto(userRepository.save(student));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
        return convertToDto(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email '" + email + "' not found"));
        return convertToDto(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByName(String name) {
        User user = userRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("User with name '" + name + "' not found"));
        return convertToDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsersPaginated(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        return convertPageToPageDto(page);
    }

    public UserResponse updateUser(@NotNull Integer id, @Valid @NotNull UserUpsertRequest dto) {
        if (dto.getRoleId() == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));

        if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("User with email '" + dto.getEmail() + "' already exists");
        }

        if (!user.getName().equals(dto.getName()) && userRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("User with name '" + dto.getName() + "' already exists");
        }

        if (dto.getTgId() != null && !dto.getTgId().equals(user.getTgId())
                && userRepository.existsByTgId(dto.getTgId())) {
            throw new DuplicateResourceException("User with Telegram ID '" + dto.getTgId() + "' already exists");
        }

        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role with id " + dto.getRoleId() + " not found"));

        user.setRole(role);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setBio(dto.getBio());
        user.setPhoto(dto.getPhoto());
        user.setTgId(dto.getTgId());

        User updatedUser = userRepository.save(user);
        return convertToDto(updatedUser);
    }

    public void deleteUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
        userRepository.delete(user);
    }

    
    
    

    @Transactional(readOnly = true)
    public User getUserEntityById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public User getUserEntityByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByName(usernameOrEmail))
                .orElseThrow(() -> new ResourceNotFoundException("User '" + usernameOrEmail + "' not found"));
    }

    public void assertUserEntityHasRole(User user, RoleName requiredRole) {
        if (user == null || user.getRole() == null || user.getRole().getRolename() == null
                || requiredRole != user.getRole().getRolename()) {
            throw new ForbiddenOperationException("User must have role " + requiredRole.name());
        }
    }

    public void assertUserEntityHasAnyRole(User user, RoleName... roles) {
        if (user == null || user.getRole() == null || user.getRole().getRolename() == null || roles == null) {
            throw new ForbiddenOperationException("User has invalid role");
        }
        RoleName actual = user.getRole().getRolename();
        for (RoleName r : roles) {
            if (r == actual) {
                return;
            }
        }
        throw new ForbiddenOperationException("User must have one of roles " + java.util.Arrays.toString(roles));
    }


    
    
    

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return userRepository.existsByName(name);
    }

    @Transactional(readOnly = true)
    public boolean existsByTgId(String tgId) {
        return tgId != null && userRepository.existsByTgId(tgId);
    }

    
    public User createStudentFromJoinRequest(String requestedName, String email, String tgId) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        if (requestedName == null || requestedName.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }

        if (existsByEmail(email)) {
            throw new DuplicateResourceException("User with email '" + email + "' already exists");
        }
        if (tgId != null && existsByTgId(tgId)) {
            throw new DuplicateResourceException("User with Telegram ID '" + tgId + "' already exists");
        }

        Role studentRole = roleRepository.findByRolename(ROLE_STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Role '" + ROLE_STUDENT + "' not found"));

        String name = requestedName.trim();
        if (existsByName(name)) {
            name = makeUniqueName(name);
        }

        User student = new User();
        student.setRole(studentRole);
        student.setName(name);
        student.setEmail(email);
        student.setTgId(tgId);
        student.setPassword(passwordEncoder.encode(generateTemporaryPassword(12)));

        return userRepository.save(student);
    }


    private void validateUserCreateCommon(UserUpsertRequest dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("User with email '" + dto.getEmail() + "' already exists");
        }

        if (userRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("User with name '" + dto.getName() + "' already exists");
        }

        if (dto.getTgId() != null && userRepository.existsByTgId(dto.getTgId())) {
            throw new DuplicateResourceException("User with Telegram ID '" + dto.getTgId() + "' already exists");
        }
    }

    private String generateTemporaryPassword(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(TEMP_PASSWORD_ALPHABET[SECURE_RANDOM.nextInt(TEMP_PASSWORD_ALPHABET.length)]);
        }
        return sb.toString();
    }

    private String makeUniqueName(String base) {
        for (int i = 1; i <= 50; i++) {
            String candidate = base + "_" + i;
            if (!existsByName(candidate)) {
                return candidate;
            }
        }
        throw new DuplicateResourceException("Unable to generate unique user name");
    }

    private UserResponse convertToDto(User user) {
        
        return new UserResponse(
                user.getId(),
                user.getRole() != null ? user.getRole().getId() : null,
                user.getName(),
                user.getEmail(),
                null,
                user.getBio(),
                user.getPhoto(),
                user.getTgId()
        );
    }

    private PageResponse<UserResponse> convertPageToPageDto(Page<User> page) {
        return new PageResponse<>(
                page.getContent().stream()
                        .map(this::convertToDto)
                        .collect(Collectors.toList()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst()
        );
    }
}
