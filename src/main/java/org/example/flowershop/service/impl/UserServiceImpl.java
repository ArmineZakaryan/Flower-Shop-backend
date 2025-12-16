package org.example.flowershop.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.LoginUserRequest;
import org.example.flowershop.dto.UpdateUserRequest;
import org.example.flowershop.dto.UserDto;
import org.example.flowershop.exception.EmailAlreadyExistsException;
import org.example.flowershop.exception.UserNotFoundException;
import org.example.flowershop.mapper.UserMapper;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.repository.UserRepository;
import org.example.flowershop.service.MailService;
import org.example.flowershop.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;


    @Override
    public User registerUser(User request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.error("Registration failed: Email already exists for {}", request.getEmail());
            throw new EmailAlreadyExistsException("Email already exists!");
        }
        request.setPassword(passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(request);
        mailService.sendWelcomeMail(savedUser);
        log.info("User registered successfully: {}", savedUser.getEmail());
        return savedUser;
    }


    @Override
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found: {}", id);
                    return new UserNotFoundException("User not found");
                });
        log.info("Updating user details for user id: {}", id);

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getSurname() != null) {
            user.setSurname(request.getSurname());
        }
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", updatedUser.getEmail());
        return userMapper.toDto(updatedUser);
    }

    @Override
    public User login(LoginUserRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .filter(userDto -> passwordEncoder.matches(request.getPassword(), userDto.getPassword()))
                .map(user -> {
                    log.info("Login successful for user: {}", user.getEmail());
                    return user;
                })

                .orElseThrow(() -> {
                    log.error("Login failed: Invalid email or password for {}", request.getEmail());

                    return new UserNotFoundException("Invalid email or password");
                });
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User save(User user) {
        log.info("Saving user: {}", user.getEmail());
        mailService.sendMail(user.getEmail(), "Welcome", "You have successfully registered");
        return userRepository.save(user);
    }
}