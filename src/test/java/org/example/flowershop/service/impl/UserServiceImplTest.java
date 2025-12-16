package org.example.flowershop.service.impl;

import org.example.flowershop.dto.LoginUserRequest;
import org.example.flowershop.dto.UpdateUserRequest;
import org.example.flowershop.dto.UserDto;
import org.example.flowershop.exception.EmailAlreadyExistsException;
import org.example.flowershop.mapper.UserMapper;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.repository.UserRepository;
import org.example.flowershop.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userServiceImpl;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MailService mailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void updateUser_shouldUpdateUser() {
        long id = 1;
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setUsername("newUsername");
        updateUserRequest.setEmail("new@email.com");

        User newUser = new User();
        newUser.setId(id);
        newUser.setUsername("username");
        newUser.setEmail("user@email.com");

        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setUsername("newUsername");
        updateUser.setEmail("new@email.com");

        UserDto userDto = new UserDto();
        userDto.setUsername("newUsername");
        userDto.setEmail("new@email.com");

        when(userRepository.findById(id)).thenReturn(Optional.of(newUser));
        when(userRepository.save(newUser)).thenReturn(updateUser);
        when(userMapper.toDto(updateUser)).thenReturn(userDto);

        UserDto updatedUser = userServiceImpl.updateUser(id, updateUserRequest);

        assertEquals("newUsername", updatedUser.getUsername());
        assertEquals("new@email.com", updatedUser.getEmail());
    }

    @Test
    void registerUser_shouldEncodePassword() {
        User requestUser = new User();
        requestUser.setEmail("user@email.com");
        requestUser.setPassword("password");

        User savedUser = new User();
        savedUser.setEmail("user@email.com");
        savedUser.setPassword("encodedPassword");

        when(userRepository.findByEmail(requestUser.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userServiceImpl.registerUser(requestUser);

        assertEquals("encodedPassword", result.getPassword());
        verify(mailService).sendWelcomeMail(savedUser);
    }

    @Test
    void registerUser_shouldThrowException_WhenEmailAlreadyExists() {
        User requestUser = new User();
        requestUser.setEmail("user@email.com");

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(new User()));

        EmailAlreadyExistsException exception = assertThrows(EmailAlreadyExistsException.class,
                () -> userServiceImpl.registerUser(requestUser));
        assertEquals("Email already exists!", exception.getMessage());
    }

    @Test
    void login_shouldReturnUser() {
        LoginUserRequest request = new LoginUserRequest();
        request.setEmail("user@email.com");
        request.setPassword("password");

        User savedUser = new User();
        savedUser.setEmail("user@email.com");
        savedUser.setPassword("encodedPassword");

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

        User result = userServiceImpl.login(request);
        assertEquals(savedUser, result);
    }

    @Test
    void login_shouldThrowException() {
        LoginUserRequest request = new LoginUserRequest();
        request.setEmail("user@email.com");
        request.setPassword("password");
        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userServiceImpl.login(request));
        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void save_shouldSaveUser() {
        User user = new User();
        user.setEmail("user@email.com");

        when(userRepository.save(user)).thenReturn(user);
        User result = userServiceImpl.save(user);

        verify(mailService).sendMail("user@email.com", "Welcome", "You have successfully registered");
        verify(userRepository).save(user);
        assertEquals(user, result);
    }

    @Test
    void findByEmail_shouldReturnUser() {
        String email = "user@email.com";
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Optional<User> result = userServiceImpl.findByEmail(email);
        assertTrue(result.isPresent());
        assertEquals(email, result.get().getEmail());
    }
}