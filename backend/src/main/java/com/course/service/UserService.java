package com.course.service;

import com.course.dto.PageDto;
import com.course.dto.UpdateProfileDto;
import com.course.dto.UserDto;
import com.course.entity.Role;
import com.course.entity.User;
import com.course.exception.DuplicateResourceException;
import com.course.exception.ForbiddenOperationException;
import com.course.exception.ResourceNotFoundException;
import com.course.repository.RoleRepository;
import com.course.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorageService avatarStorageService;

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_METHODIST = "METHODIST";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_STUDENT = "STUDENT";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] TEMP_PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%".toCharArray();

    /**
     * Business operation:
     * TEACHER, METHODIST or STUDENT can update own profile.
     * Email and Telegram ID cannot be changed yet.
     */
    public UserDto updateOwnProfile(User currentUser, UpdateProfileDto dto) {
        if (currentUser == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }

        // Only TEACHER, METHODIST or STUDENT
        boolean isTeacher = currentUser.getRole() != null
                && ROLE_TEACHER.equalsIgnoreCase(currentUser.getRole().getRolename());
        boolean isMethodist = currentUser.getRole() != null
                && ROLE_METHODIST.equalsIgnoreCase(currentUser.getRole().getRolename());
        boolean isStudent = currentUser.getRole() != null
                && ROLE_STUDENT.equalsIgnoreCase(currentUser.getRole().getRolename());
        if (!isTeacher && !isMethodist && !isStudent) {
            throw new ForbiddenOperationException("Only TEACHER, METHODIST or STUDENT can update profile");
        }

        // name
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

        // bio (can be set to empty string if needed)
        if (dto.getBio() != null) {
            currentUser.setBio(dto.getBio());
        }
        // photo is managed via S3 avatar endpoints, not via profile update

        // password (optional)
        if (dto.getPassword() != null) {
            String newPassword = dto.getPassword();
            if (newPassword.isBlank()) {
                throw new IllegalArgumentException("Password cannot be blank");
            }
            currentUser.setPassword(passwordEncoder.encode(newPassword));
        }

        User saved = userRepository.save(currentUser);
        return convertToDto(saved);
    }

    /**
     * TEACHER/METHODIST/STUDENT: upload (or replace) own avatar.
     * Validates file format & size in AvatarStorageService.
     */
    public UserDto uploadOwnAvatar(User currentUser, org.springframework.web.multipart.MultipartFile file) {
        if (currentUser == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }

        boolean isTeacher = currentUser.getRole() != null
                && ROLE_TEACHER.equalsIgnoreCase(currentUser.getRole().getRolename());
        boolean isMethodist = currentUser.getRole() != null
                && ROLE_METHODIST.equalsIgnoreCase(currentUser.getRole().getRolename());
        boolean isStudent = currentUser.getRole() != null
                && ROLE_STUDENT.equalsIgnoreCase(currentUser.getRole().getRolename());
        if (!isTeacher && !isMethodist && !isStudent) {
            throw new ForbiddenOperationException("Only TEACHER, METHODIST or STUDENT can upload avatar");
        }

        // delete previous avatar if it was stored in our S3 bucket
        avatarStorageService.deleteByPublicUrl(currentUser.getPhoto());

        String publicUrl = avatarStorageService.uploadAvatar(currentUser.getId(), file);
        currentUser.setPhoto(publicUrl);
        return convertToDto(userRepository.save(currentUser));
    }

    /**
     * TEACHER/METHODIST/STUDENT: delete own avatar (only if it was stored in our S3 bucket).
     */
    public UserDto deleteOwnAvatar(User currentUser) {
        if (currentUser == null) {
            throw new ForbiddenOperationException("Unauthenticated");
        }

        boolean isTeacher = currentUser.getRole() != null
                && ROLE_TEACHER.equalsIgnoreCase(currentUser.getRole().getRolename());
        boolean isMethodist = currentUser.getRole() != null
                && ROLE_METHODIST.equalsIgnoreCase(currentUser.getRole().getRolename());
        boolean isStudent = currentUser.getRole() != null
                && ROLE_STUDENT.equalsIgnoreCase(currentUser.getRole().getRolename());
        if (!isTeacher && !isMethodist && !isStudent) {
            throw new ForbiddenOperationException("Only TEACHER, METHODIST or STUDENT can delete avatar");
        }

        avatarStorageService.deleteByPublicUrl(currentUser.getPhoto());
        currentUser.setPhoto(null);
        return convertToDto(userRepository.save(currentUser));
    }

    /**
     * Business operation:
     * METHODIST can register a new TEACHER in the system.
     *
     * NOTE: This method does NOT rely on authentication yet.
     * The caller must provide metodistUserId.
     */
    public UserDto createTeacherByMethodist(Integer metodistUserId, UserDto dto) {
        assertUserHasRole(metodistUserId, ROLE_METHODIST);

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
        return convertToDto(saved);
    }

    /**
     * Business operation:
     * METHODIST can delete only TEACHER users.
     */
    public void deleteTeacherByMethodist(Integer metodistUserId, Integer teacherUserId) {
        assertUserHasRole(metodistUserId, ROLE_METHODIST);

        User teacher = userRepository.findById(teacherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + teacherUserId + " not found"));

        if (teacher.getRole() == null || teacher.getRole().getRolename() == null
                || !ROLE_TEACHER.equalsIgnoreCase(teacher.getRole().getRolename())) {
            throw new ForbiddenOperationException("Methodist can delete only TEACHER users");
        }

        userRepository.delete(teacher);
    }

    /**
     * Admin operation:
     * create a new METHODIST.
     *
     * Role is enforced on the backend.
     */
    public UserDto createMethodist(UserDto dto) {
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

    /**
     * Admin operation:
     * delete only METHODIST users.
     */
    public void deleteMethodist(Integer methodistUserId) {
        User methodist = userRepository.findById(methodistUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + methodistUserId + " not found"));

        if (methodist.getRole() == null || methodist.getRole().getRolename() == null
                || !ROLE_METHODIST.equalsIgnoreCase(methodist.getRole().getRolename())) {
            throw new ForbiddenOperationException("Admin can delete only METHODIST users via this endpoint");
        }

        userRepository.delete(methodist);
    }

    /**
     * Admin operation:
     * change password of the currently authenticated ADMIN.
     */
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
                || !ROLE_ADMIN.equalsIgnoreCase(admin.getRole().getRolename())) {
            throw new ForbiddenOperationException("Only ADMIN can change admin password");
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(admin);
    }

    private void assertUserHasRole(Integer userId, String requiredRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));

        if (user.getRole() == null || user.getRole().getRolename() == null
                || !requiredRole.equalsIgnoreCase(user.getRole().getRolename())) {
            throw new ForbiddenOperationException("User with id " + userId + " must have role " + requiredRole);
        }
    }

    /**
     * Helper for other services: convert entity to safe DTO (password is always null).
     */
    public UserDto toDto(User user) {
        return convertToDto(user);
    }

    /**
     * Generic create (admin-like) endpoint: create a user with the role supplied via roleId.
     */
    public UserDto createUser(UserDto dto) {
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

    @Transactional(readOnly = true)
    public UserDto getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
        return convertToDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email '" + email + "' not found"));
        return convertToDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByName(String name) {
        User user = userRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("User with name '" + name + "' not found"));
        return convertToDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageDto<UserDto> getAllUsersPaginated(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        return convertPageToPageDto(page);
    }

    public UserDto updateUser(Integer id, UserDto dto) {
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

    // -----------------------------
    // Entity-level helpers (used by other services)
    // -----------------------------

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

    public void assertUserEntityHasRole(User user, String requiredRole) {
        if (user == null || user.getRole() == null || user.getRole().getRolename() == null
                || !requiredRole.equalsIgnoreCase(user.getRole().getRolename())) {
            throw new ForbiddenOperationException("User must have role " + requiredRole);
        }
    }


    // -----------------------------
    // Lightweight checks for other services (avoid direct repository injection)
    // -----------------------------

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

    /**
     * Business operation used by ClassJoinRequestService:
     * create a new STUDENT user from an approved join request.
     *
     * - validates uniqueness of email and tgId
     * - if name already exists, makes it unique
     * - generates a temporary password (encoded)
     */
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


    private void validateUserCreateCommon(UserDto dto) {
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

    private UserDto convertToDto(User user) {
        // Do NOT expose password.
        return new UserDto(
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

    private PageDto<UserDto> convertPageToPageDto(Page<User> page) {
        return new PageDto<>(
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
