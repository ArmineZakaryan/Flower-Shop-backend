package org.example.flowershop.service.impl;

import org.example.flowershop.dto.LoginUserRequest;
import org.example.flowershop.dto.SaveUserRequest;
import org.example.flowershop.dto.UpdateUserRequest;
import org.example.flowershop.dto.UserAuthResponse;
import org.example.flowershop.dto.UserDto;
import org.example.flowershop.exception.EmailAlreadyExistsException;
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
import org.example.flowershop.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userServiceImpl;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MailService mailService;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerUser_success() {
        SaveUserRequest request = new SaveUserRequest();
        request.setEmail("user@mail.com");
        request.setUsername("user");
        request.setPassword("password");

        User user = new User();
        user.setId(1L);
        user.setEmail("user@mail.com");

        UserDto dto = new UserDto();

        when(userRepository.findByEmail("user@mail.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());
        when(userMapper.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(dto);

        UserDto result = userServiceImpl.registerUser(request);

        assertNotNull(result);
        verify(mailService).sendWelcomeMail(user);
    }

    @Test
    void registerUser_emailAlreadyExists() {
        SaveUserRequest request = new SaveUserRequest();
        request.setEmail("user@mail.com");
        request.setUsername("user");
        request.setPassword("password123");

        when(userRepository.findByEmail("user@mail.com"))
                .thenReturn(Optional.of(new User()));

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userServiceImpl.registerUser(request)
        );
    }

    @Test
    void registerUser_usernameAlreadyExists() {
        SaveUserRequest request = new SaveUserRequest();
        request.setEmail("user@mail.com");
        request.setUsername("user");
        request.setPassword("password123");

        when(userRepository.findByEmail("user@mail.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("user"))
                .thenReturn(Optional.of(new User()));

        assertThrows(
                UsernameAlreadyExistsException.class,
                () -> userServiceImpl.registerUser(request)
        );
    }

    @Test
    void updateUser_success() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("newUsername");

        User user = new User();
        user.setId(1L);

        UserDto dto = new UserDto();
        dto.setUsername("newUsername");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dto);

        UserDto result = userServiceImpl.updateUser(1L, request);

        assertEquals("newUsername", result.getUsername());
    }

    @Test
    void updateUser_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userServiceImpl.updateUser(1L, new UpdateUserRequest())
        );
    }


    @Test
    void login_success() {
        LoginUserRequest request = new LoginUserRequest();
        request.setEmail("user@mail.com");
        request.setPassword("password");

        User user = new User();
        user.setId(1L);
        user.setEmail("user@mail.com");
        user.setPassword("encoded");

        when(userRepository.findByEmail("user@mail.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded"))
                .thenReturn(true);
        when(jwtTokenUtil.generateToken("user@mail.com"))
                .thenReturn("token");

        UserAuthResponse response = userServiceImpl.login(request);

        assertEquals("token", response.getToken());
        assertEquals(1L, response.getUserId());
    }

    @Test
    void login_wrongPassword() {
        LoginUserRequest request = new LoginUserRequest();
        request.setEmail("user@mail.com");
        request.setPassword("wrong");

        User user = new User();
        user.setPassword("encoded");

        when(userRepository.findByEmail("user@mail.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded"))
                .thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> userServiceImpl.login(request)
        );
    }


    @Test
    void getAllUsers_validSort() {
        User user = new User();

        when(userRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(user));
        when(userMapper.toDto(user))
                .thenReturn(new UserDto());

        List<UserDto> result = userServiceImpl.getAllUsers("username");

        assertEquals(1, result.size());
    }


    @Test
    void delete_selfAllowed() {
        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.existsByUserId(1L)).thenReturn(false);
        when(favoriteRepository.existsByUserId(1L)).thenReturn(false);
        when(cartItemRepository.existsByUserId(1L)).thenReturn(false);

        assertDoesNotThrow(() ->
                userServiceImpl.delete(1L, user)
        );

        verify(userRepository).delete(user);
    }

    @Test
    void delete_otherUser_byAdmin() {
        User admin = new User();
        admin.setId(1L);
        admin.setUserType(UserType.ADMIN);

        User user = new User();
        user.setId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(orderRepository.existsByUserId(2L)).thenReturn(false);
        when(favoriteRepository.existsByUserId(2L)).thenReturn(false);
        when(cartItemRepository.existsByUserId(2L)).thenReturn(false);

        assertDoesNotThrow(() ->
                userServiceImpl.delete(2L, admin)
        );

        verify(userRepository).delete(user);
    }

    @Test
    void delete_otherUser_byNonAdmin_shouldFail() {
        User current = new User();
        current.setId(1L);
        current.setUserType(UserType.USER);

        User user = new User();
        user.setId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        assertThrows(
                AccessDeniedException.class,
                () -> userServiceImpl.delete(2L, current)
        );
    }
}