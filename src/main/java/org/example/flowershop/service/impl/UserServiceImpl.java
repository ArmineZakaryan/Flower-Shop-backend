package org.example.flowershop.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.LoginUserRequest;
import org.example.flowershop.dto.SaveUserRequest;
import org.example.flowershop.dto.UpdateUserRequest;
import org.example.flowershop.dto.UserAuthResponse;
import org.example.flowershop.dto.UserDto;
import org.example.flowershop.exception.EmailAlreadyExistsException;
import org.example.flowershop.exception.UserHasRelationsException;
import org.example.flowershop.exception.UserNotFoundException;
import org.example.flowershop.exception.UsernameAlreadyExistsException;
import org.example.flowershop.mapper.UserMapper;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.model.enums.UserType;
import org.example.flowershop.repository.CartItemRepository;
import org.example.flowershop.repository.FavoriteRepository;
import org.example.flowershop.repository.OrderRepository;
import org.example.flowershop.repository.UserRepository;
import org.example.flowershop.service.MailService;
import org.example.flowershop.service.UserService;
import org.example.flowershop.util.JwtTokenUtil;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final JwtTokenUtil tokenUtil;

    @Override
    public UserDto registerUser(SaveUserRequest request) {
        log.info("Registering user email={}", request.getEmail());
        userRepository.findByEmail(request.getEmail())
                .ifPresent(u -> {
                    log.warn("Registration failed: email already exists: {}", request.getEmail());
                    throw new EmailAlreadyExistsException("Email already exists");
                });

        userRepository.findByUsername(request.getUsername())
                .ifPresent(u -> {
                    log.warn("Registration failed: username already exists: {}", request.getUsername());
                    throw new UsernameAlreadyExistsException("Username already exists");
                });

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        try {
            mailService.sendWelcomeMail(savedUser);
        } catch (Exception e) {
            log.error("Failed to send welcome mail to userId={}", savedUser.getId(), e);
        }

        log.info("Registration completed successfully for user id={}", savedUser.getId());
        return userMapper.toDto(savedUser);
    }

    @Override
    public UserDto updateUser(long id, UpdateUserRequest request) {
        log.info("Updating user id={}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found for update id={}", id);
                    return new UserNotFoundException("User not found");
                });

        if (request.getEmail() != null && userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }
        if (request.getUsername() != null && userRepository.existsByUsernameAndIdNot(request.getUsername(), id)) {
            throw new UsernameAlreadyExistsException("Username already exists");
        }

        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getName() != null) user.setName(request.getName());
        if (request.getSurname() != null) user.setSurname(request.getSurname());
        if (request.getUsername() != null) user.setUsername(request.getUsername());

        log.info("User updated successfully id={}", id);
        return userMapper.toDto(user);
    }

    @Override
    public UserAuthResponse login(LoginUserRequest request) {
        log.info("Login attempt for email={}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed for email={}", request.getEmail());
                    return new BadCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: invalid password for email={}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        log.info("Login successful for user id={}", user.getId());

        return UserAuthResponse.builder()
                .token(tokenUtil.generateToken(user.getEmail()))
                .name(user.getName())
                .surname(user.getSurname())
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers(String sort) {
        log.info("Fetching all users sorted by {}", sort);

        Set<String> allowed = Set.of("username", "email", "name");
        if (sort == null || !allowed.contains(sort)) {
            log.warn("Invalid sort field '{}', defaulting to 'username'", sort);
            sort = "username";
        }

        return userRepository.findAll(Sort.by(sort))
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email {}", email);
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(long id) {
        log.info("Fetching user by id={}", id);

        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> {
                    log.warn("User not found id={}", id);
                    return new UserNotFoundException("User not found");
                });
    }

    @Override
    @Transactional
    public void delete(long id, User currentUser) {
        log.info("Delete request for user id={} by user id={}", id, currentUser.getId());

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!currentUser.getId().equals(id) && currentUser.getUserType() != UserType.ADMIN) {
            throw new AccessDeniedException("Not allowed");
        }

        if (orderRepository.existsByUserId(id)) {
            throw new UserHasRelationsException(
                    "Cannot delete user with id: " + id + " because user has orders"
            );
        }

        if (favoriteRepository.existsByUserId(id)) {
            throw new UserHasRelationsException(
                    "Cannot delete user with id: " + id + " because user has favorites"
            );
        }

        if (cartItemRepository.existsByUserId(id)) {
            throw new UserHasRelationsException(
                    "Cannot delete user with id: " + id + " because user has cart items"
            );
        }

        userRepository.delete(user);
        log.info("User deleted successfully id={}", id);
    }
}