package org.example.flowershop.endpoint;

import org.example.flowershop.dto.LoginUserRequest;
import org.example.flowershop.dto.SaveUserRequest;
import org.example.flowershop.dto.UpdateUserRequest;
import org.example.flowershop.dto.UserDto;
import org.example.flowershop.exception.EmailAlreadyExistsException;
import org.example.flowershop.exception.ProductNotFoundException;
import org.example.flowershop.exception.UserNotFoundException;
import org.example.flowershop.exception.UsernameAlreadyExistsException;
import org.example.flowershop.mapper.UserMapper;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.model.enums.UserType;
import org.example.flowershop.repository.UserRepository;
import org.example.flowershop.security.CurrentUser;
import org.example.flowershop.service.UserService;
import org.example.flowershop.util.JwtTokenUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class UserEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    User testUser = new User(1L, "Jon", "asdf", "Jon11", "jon.@email.com", "jon1122", UserType.USER);

    @Test
    void getAllUsers_shouldUseDefaultSorting() throws Exception {
        User user = new User(
                1L,
                "John",
                "Doe",
                "john",
                "john@email.com",
                "password",
                UserType.USER
        );

        UserDto dto = new UserDto(
                1L,
                "John",
                "Doe",
                "john",
                "john@email.com",
                "USER"
        );

        when(userRepository.findAll(Sort.by("username")))
                .thenReturn(List.of(user));

        when(userMapper.toDto(user))
                .thenReturn(dto);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("john"));

        verify(userRepository).findAll(Sort.by("username"));
        verify(userMapper).toDto(user);
    }


    @Test
    void getUserById_shouldReturnUserDto_whenUserExists() throws Exception {
        long userId = 1L;
        UserDto userDto = new UserDto(userId, "John", "Doe", "john", "john@email.com", "USER");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userMapper.toDto(testUser)).thenReturn(userDto);

        mockMvc.perform(get("/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.email").value("john@email.com"));

        verify(userRepository).findById(userId);
        verify(userMapper).toDto(testUser);
    }

    @Test
    void getUserById_shouldThrowException_whenUserNotFound() throws Exception {
        long userId = 1L;

        when(userRepository.findById(userId))
                .thenThrow(new ProductNotFoundException("User with id " + userId + " not found"));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldCreateUserSuccessfully() throws Exception {
        SaveUserRequest request = new SaveUserRequest();
        request.setEmail("unique@example.com");
        request.setUsername("newuser");
        request.setPassword("password");

        User userEntity = new User();
        userEntity.setEmail("unique@example.com");
        userEntity.setUsername("newuser");
        userEntity.setPassword("encoded_password");

        UserDto userDto = new UserDto();
        userDto.setEmail("unique@example.com");
        userDto.setUsername("newuser");

        when(userRepository.findByEmail("unique@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded_password");
        when(userMapper.toEntity(any(SaveUserRequest.class))).thenReturn(userEntity);
        when(userRepository.save(userEntity)).thenReturn(userEntity);
        when(userMapper.toDto(userEntity)).thenReturn(userDto);

        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("unique@example.com"))
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void create_shouldThrowEmailAlreadyExistsException() throws Exception {
        SaveUserRequest request = new SaveUserRequest();
        request.setEmail("test@example.com");
        request.setUsername("newuser");
        request.setPassword("password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(new User()));

        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof EmailAlreadyExistsException));
    }


    @Test
    void create_shouldThrowUsernameAlreadyExistsException() throws Exception {
        SaveUserRequest request = new SaveUserRequest();
        request.setEmail("aaaa@email.com");
        request.setUsername("aaaa");
        request.setPassword("aaa11");

        when(userRepository.findByEmail("aaaa@email.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("aaaa")).thenReturn(Optional.of(new User()));

        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof UsernameAlreadyExistsException)
                );
    }


    @Test
    void update_shouldUpdateUserSuccessfully() throws Exception {
        long userId = 1L;
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("New Name");
        request.setSurname("New Surname");
        request.setUsername("newUsername");
        request.setEmail("newEmail@example.com");

        User currentUser = new User(userId, "Jon", "asdf", "Jon11", "jon.@email.com", "jon1122", UserType.USER);
        UserDto userDto = new UserDto(userId, "New Name", "New Surname", "newUsername", "newEmail@example.com", "USER");

        CurrentUser currentUserDetails = new CurrentUser(currentUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUserDetails, null, currentUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userService.updateUser(userId, request)).thenReturn(userDto);

        mockMvc.perform(MockMvcRequestBuilders.put("/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newUsername"))
                .andExpect(jsonPath("$.email").value("newEmail@example.com"))
                .andExpect(jsonPath("$.name").value("New Name"));

        verify(userService).updateUser(userId, request);
    }

    @Test
    void update_shouldReturnNotFound_whenUserNotExists() throws Exception {
        long userId = 1L;
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("New Name");
        request.setSurname("New Surname");
        request.setUsername("newUsername");
        request.setEmail("newEmail@example.com");

        User currentUser = new User(userId, "Jon", "asdf", "Jon11", "jon.@email.com", "jon1122", UserType.USER);
        CurrentUser currentUserDetails = new CurrentUser(currentUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUserDetails, null, currentUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userService.updateUser(userId, request))
                .thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(MockMvcRequestBuilders.put("/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .principal(authentication))
                .andExpect(status().isNotFound());

        verify(userService).updateUser(userId, request);
    }


    @Test
    void delete_shouldDeleteOwnUserSuccessfully_whenUserDeletesOwnAccount() throws Exception {
        long userId = 1L;
        User currentUser = new User(userId, "john", "John Doe", "john", "john@email.com", "password", UserType.USER);
        User user = new User(userId, "john", "John Doe", "john", "john@email.com", "password", UserType.USER);

        CurrentUser currentUserDetails = new CurrentUser(currentUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUserDetails, null, currentUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);


        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        mockMvc.perform(delete("/users/{id}", userId)
                        .principal(new UsernamePasswordAuthenticationToken(testUser, null)))
                .andExpect(status().isNoContent());

        verify(userRepository).delete(user);
    }

    @Test
    void delete_shouldDeleteUserSuccessfully_whenAdminDeletesUser() throws Exception {

        long userId = 1L;
        User adminUser = new User(100L, "admin", "Admin", "admin",
                "admin@email.com", "adminPass", UserType.ADMIN);

        User user = new User(userId, "john", "John Doe", "john",
                "john@email.com", "password", UserType.USER);

        CurrentUser currentUserDetails = new CurrentUser(adminUser);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        currentUserDetails, null, currentUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        mockMvc.perform(delete("/users/{id}", userId)
                        .principal(new UsernamePasswordAuthenticationToken(testUser, null)))
                .andExpect(status().isNoContent());

        verify(userRepository).delete(user);
    }


    @Test
    void delete_shouldReturnForbidden_whenUserTriesToDeleteAnotherUser() throws Exception {
        long userId = 1L;
        User currentUser = new User(2L, "john", "John Doe", "john", "john@email.com", "password", UserType.USER);
        User userToDelete = new User(userId, "jack", "Jack Doe", "jack", "jack@email.com", "password", UserType.USER);

        CurrentUser currentUserDetails = new CurrentUser(currentUser);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUserDetails, null, currentUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);


        when(userRepository.findById(userId)).thenReturn(Optional.of(userToDelete));

        mockMvc.perform(delete("/users/{id}", userId)
                        .principal(new UsernamePasswordAuthenticationToken(currentUser, null)))
                .andExpect(status().isForbidden());

        verify(userRepository, never()).delete(userToDelete);
    }


    @Test
    void register_shouldReturnOk_whenEmailDoesNotExist() throws Exception {
        SaveUserRequest request = new SaveUserRequest();
        request.setEmail("newuser@email.com");
        request.setUsername("newuser");
        request.setPassword("password123");

        when(userService.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        when(userMapper.toEntity(any(SaveUserRequest.class))).thenAnswer(invocation -> {
            SaveUserRequest req = invocation.getArgument(0);
            User u = new User();
            u.setEmail(req.getEmail());
            u.setUsername(req.getUsername());
            u.setPassword(req.getPassword());
            return u;
        });

        when(userService.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(MockMvcRequestBuilders.post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(passwordEncoder).encode("password123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("encodedPassword", savedUser.getPassword());

        verify(userService).findByEmail(request.getEmail());
        verify(userMapper).toEntity(request);
    }

    @Test
    void register_shouldEncodePasswordBeforeSaving() throws Exception {
        SaveUserRequest request = new SaveUserRequest();
        request.setEmail("newuser@email.com");
        request.setUsername("newuser");
        request.setPassword("password123");

        when(userService.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        when(userMapper.toEntity(any(SaveUserRequest.class))).thenAnswer(invocation -> {
            SaveUserRequest req = invocation.getArgument(0);
            User u = new User();
            u.setEmail(req.getEmail());
            u.setUsername(req.getUsername());
            u.setPassword(req.getPassword());
            return u;
        });

        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        when(userService.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(MockMvcRequestBuilders.post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(passwordEncoder).encode("password123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("encodedPassword", savedUser.getPassword());

        verify(userService).findByEmail(request.getEmail());
        verify(userMapper).toEntity(request);
    }

    @Test
    void login_shouldReturnOk_whenCredentialsAreCorrect() throws Exception {
        LoginUserRequest request = new LoginUserRequest();
        request.setEmail("test@email.com");
        request.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setPassword("encodedPassword");
        user.setName("John");
        user.setSurname("Doe");

        when(userService.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtTokenUtil.generateToken(user.getEmail())).thenReturn("dummyToken");

        mockMvc.perform(MockMvcRequestBuilders.post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("dummyToken"))
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.surname").value("Doe"))
                .andExpect(jsonPath("$.userId").value("1"));

        verify(userService).findByEmail(request.getEmail());
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(jwtTokenUtil).generateToken(user.getEmail());
    }

    @Test
    void login_shouldReturnUnauthorized_whenUserDoesNotExist() throws Exception {
        LoginUserRequest request = new LoginUserRequest();
        request.setEmail("nonexistent@email.com");
        request.setPassword("password123");

        when(userService.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(userService).findByEmail(request.getEmail());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtTokenUtil, never()).generateToken(any());
    }

    @Test
    void login_shouldReturnUnauthorized_whenPasswordIsIncorrect() throws Exception {
        LoginUserRequest request = new LoginUserRequest();
        request.setEmail("test@email.com");
        request.setPassword("wrongPassword");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setPassword("encodedPassword");

        when(userService.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        mockMvc.perform(MockMvcRequestBuilders.post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(userService).findByEmail(request.getEmail());
        verify(passwordEncoder).matches("wrongPassword", "encodedPassword");
        verify(jwtTokenUtil, never()).generateToken(any());
    }
}