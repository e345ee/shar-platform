package com.course.service;

import com.course.dto.CreateUserDto;
import com.course.dto.PageDto;
import com.course.dto.UserDto;
import com.course.entity.Role;
import com.course.entity.User;
import com.course.exception.DuplicateResourceException;
import com.course.exception.ResourceNotFoundException;
import com.course.repository.RoleRepository;
import com.course.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserDto createUser(CreateUserDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("User with email '" + dto.getEmail() + "' already exists");
        }

        if (userRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("User with name '" + dto.getName() + "' already exists");
        }

        if (dto.getTgId() != null && userRepository.existsByTgId(dto.getTgId())) {
            throw new DuplicateResourceException("User with Telegram ID '" + dto.getTgId() + "' already exists");
        }

        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role with id " + dto.getRoleId() + " not found"));

        User user = new User();
        user.setRole(role);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
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

    public UserDto updateUser(Integer id, CreateUserDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));

        if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("User with email '" + dto.getEmail() + "' already exists");
        }

        if (!user.getName().equals(dto.getName()) && userRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("User with name '" + dto.getName() + "' already exists");
        }

        if (dto.getTgId() != null && !dto.getTgId().equals(user.getTgId()) && 
            userRepository.existsByTgId(dto.getTgId())) {
            throw new DuplicateResourceException("User with Telegram ID '" + dto.getTgId() + "' already exists");
        }

        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role with id " + dto.getRoleId() + " not found"));

        user.setRole(role);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
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

    private UserDto convertToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getRole().getId(),
                user.getName(),
                user.getEmail(),
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
