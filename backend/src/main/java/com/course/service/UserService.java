package com.course.service;

import com.course.dto.auth.UserRegisterRequest;
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
import com.course.repository.MethodistTeacherRepository;
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
    private final MethodistTeacherRepository methodistTeacherRepository;
    private final StudyClassRepository studyClassRepository;

    private static final RoleName ROLE_ADMIN = RoleName.ADMIN;
    private static final RoleName ROLE_METHODIST = RoleName.METHODIST;
    private static final RoleName ROLE_TEACHER = RoleName.TEACHER;
    private static final RoleName ROLE_STUDENT = RoleName.STUDENT;


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

        if (dto.getEmail() != null) {
            String newEmail = dto.getEmail().trim();
            if (newEmail.isBlank()) {
                throw new IllegalArgumentException("Email cannot be blank");
            }
            if (!newEmail.equals(currentUser.getEmail()) && userRepository.existsByEmail(newEmail)) {
                throw new DuplicateResourceException("User with email '" + newEmail + "' already exists");
            }
            currentUser.setEmail(newEmail);
        }

        if (dto.getTgId() != null) {
            String newTgId = dto.getTgId().trim();
            if (newTgId.isBlank()) {
                currentUser.setTgId(null);
            } else {
                if (!newTgId.equals(currentUser.getTgId()) && userRepository.existsByTgId(newTgId)) {
                    throw new DuplicateResourceException("User with Telegram ID '" + newTgId + "' already exists");
                }
                currentUser.setTgId(newTgId);
            }
        }

        if (dto.getBio() != null) {
            currentUser.setBio(dto.getBio());
        }

        if (dto.getPassword() != null) {
            if (dto.getPassword().isBlank()) {
                throw new IllegalArgumentException("Password cannot be blank");
            }
            if (dto.getPassword().length() > 127) {
                throw new IllegalArgumentException("Password must be between 6 and 127 characters");
            }
            currentUser.setPassword(passwordEncoder.encode(dto.getPassword()));
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


    public UserResponse createTeacherByMethodist(@NotNull Integer metodistUserId, @Valid @NotNull UserRegisterRequest dto) {
        assertUserHasRole(metodistUserId, ROLE_METHODIST);
        User methodist = userRepository.findById(metodistUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + metodistUserId + " not found"));
        validateUserRegisterRequest(dto);
        String hashedPassword = passwordEncoder.encode(dto.getPassword());
        String tgId = (dto.getTgId() != null && !dto.getTgId().isBlank()) ? dto.getTgId() : null;
        Integer userId = userRepository.registerUser(
                dto.getName(),
                dto.getEmail(),
                hashedPassword,
                ROLE_TEACHER.name(),
                tgId
        );
        User teacher = getUserEntityById(userId);
        methodistTeacherService.linkTeacher(methodist, teacher);
        return convertToDto(teacher);
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
        teacher.setDeleted(true);
        userRepository.save(teacher);
    }

    public void restoreTeacherByMethodist(Integer metodistUserId, Integer teacherUserId) {
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
            throw new ForbiddenOperationException("Methodist can restore only TEACHER users");
        }

        if (!teacher.isDeleted()) {
            return;
        }

        teacher.setDeleted(false);
        userRepository.save(teacher);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listTeachersByMethodist(Integer methodistUserId, Pageable pageable) {
        assertUserHasRole(methodistUserId, ROLE_METHODIST);
        Page<User> page = methodistTeacherRepository.findTeachersByMethodistId(methodistUserId, pageable);
        return convertPageToPageDto(page);
    }


    public UserResponse createMethodist(@Valid @NotNull UserRegisterRequest dto) {
        validateUserRegisterRequest(dto);
        String hashedPassword = passwordEncoder.encode(dto.getPassword());
        String tgId = (dto.getTgId() != null && !dto.getTgId().isBlank()) ? dto.getTgId() : null;
        Integer userId = userRepository.registerUser(
                dto.getName(),
                dto.getEmail(),
                hashedPassword,
                ROLE_METHODIST.name(),
                tgId
        );
        return getUserById(userId);
    }


    public void deleteMethodist(Integer methodistUserId) {
        User methodist = userRepository.findById(methodistUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + methodistUserId + " not found"));

        if (methodist.getRole() == null || methodist.getRole().getRolename() == null
                || methodist.getRole().getRolename() != ROLE_METHODIST) {
            throw new ForbiddenOperationException("Admin can delete only METHODIST users via this endpoint");
        }

        methodist.setDeleted(true);
        userRepository.save(methodist);
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

        if (user.isDeleted()) {
            throw new ResourceNotFoundException("User with id " + userId + " not found");
        }

        if (user.getRole() == null || user.getRole().getRolename() == null
                || requiredRole != user.getRole().getRolename()) {
            throw new ForbiddenOperationException("User with id " + userId + " must have role " + requiredRole.name());
        }
    }


    public UserResponse toDto(User user) {
        return convertToDto(user);
    }


//        public UserResponse createUser(@Valid @NotNull UserRegisterRequest dto) {
//        if (dto.getRoleId() == null) {
//            throw new IllegalArgumentException("Role ID cannot be null");
//        }
//
//        validateUserRegisterRequest(dto);
//
//        Role role = roleRepository.findById(dto.getRoleId())
//                .orElseThrow(() -> new ResourceNotFoundException("Role with id " + dto.getRoleId() + " not found"));
//
//        String hashedPassword = passwordEncoder.encode(dto.getPassword());
//        String tgId = (dto.getTgId() != null && !dto.getTgId().isBlank()) ? dto.getTgId() : null;
//        Integer userId = userRepository.registerUser(
//                dto.getName(),
//                dto.getEmail(),
//                hashedPassword,
//                role.getRolename().name(),
//                tgId
//        );
//        return getUserById(userId);
//    }


    public UserResponse createStudent(@Valid @NotNull UserRegisterRequest dto) {
        validateUserRegisterRequest(dto);
        String hashedPassword = passwordEncoder.encode(dto.getPassword());
        String tgId = (dto.getTgId() != null && !dto.getTgId().isBlank()) ? dto.getTgId() : null;
        Integer userId = userRepository.registerUser(
                dto.getName(),
                dto.getEmail(),
                hashedPassword,
                ROLE_STUDENT.name(),
                tgId
        );
        return getUserById(userId);
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


    @Transactional(readOnly = true)
    public List<UserResponse> getAllMethodists() {
        return userRepository.findAllByRole_RolenameAndDeletedFalseOrderByNameAsc(ROLE_METHODIST).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
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
        user.setDeleted(true);
        userRepository.save(user);
    }





    @Transactional(readOnly = true)
    public User getUserEntityById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
        if (user.isDeleted()) {
            throw new ResourceNotFoundException("User with id " + id + " not found");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User getUserEntityByUsernameOrEmail(String usernameOrEmail) {
        User user = userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByName(usernameOrEmail))
                .orElseThrow(() -> new ResourceNotFoundException("User '" + usernameOrEmail + "' not found"));
        if (user.isDeleted()) {
            throw new ResourceNotFoundException("User '" + usernameOrEmail + "' not found");
        }
        return user;
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

    private void validateUserRegisterRequest(UserRegisterRequest dto) {
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