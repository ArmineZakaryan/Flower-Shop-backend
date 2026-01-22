package org.example.flowershop.endpoint;

import org.example.flowershop.dto.LoginUserRequest;
import org.example.flowershop.dto.SaveUserRequest;
import org.example.flowershop.dto.UpdateUserRequest;
import org.example.flowershop.dto.UserAuthResponse;
import org.example.flowershop.dto.UserDto;
import org.example.flowershop.exception.UserNotFoundException;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.model.enums.UserType;
import org.example.flowershop.security.CurrentUser;
import org.example.flowershop.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class UserEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    @MockitoBean
    private UserService userService;


    User testUser = new User(1L, "Jon", "asdf", "Jon11", "jon.@email.com", "jon1122", UserType.USER);

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John");
        testUser.setSurname("Doe");
        testUser.setEmail("john@mail.com");
        testUser.setPassword("password");
        testUser.setUserType(UserType.USER);
    }

    private void authenticate(User user) {
        CurrentUser currentUser = new CurrentUser(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        currentUser,
                        null,
                        currentUser.getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getAllUsers_shouldReturnUsers() throws Exception {
        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@mail.com");
        adminUser.setPassword("password");
        adminUser.setUserType(UserType.ADMIN);
        authenticate(adminUser);

        UserDto dto = UserDto.builder()
                .id(1L)
                .email("john@mail.com")
                .name("John")
                .surname("Doe")
                .build();

        when(userService.getAllUsers(""))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("john@mail.com"));

        verify(userService).getAllUsers("");
    }

    @Test
    void getUserById_shouldReturnUser() throws Exception {
        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@mail.com");
        adminUser.setPassword("password");
        adminUser.setUserType(UserType.ADMIN);
        authenticate(adminUser);

        UserDto dto = UserDto.builder()
                .id(1L)
                .email("john@mail.com")
                .build();

        when(userService.getUserById(1L)).thenReturn(dto);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(userService).getUserById(1L);
    }

    @Test
    void getUserById_whenNotFound_shouldReturn404() throws Exception {
        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@mail.com");
        adminUser.setPassword("password");
        adminUser.setUserType(UserType.ADMIN);
        authenticate(adminUser);

        when(userService.getUserById(1L))
                .thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void register_shouldReturnCreated() throws Exception {
        SaveUserRequest request = new SaveUserRequest();
        request.setEmail("test@mail.com");
        request.setUsername("test");
        request.setPassword("password123");

        UserDto dto = UserDto.builder()
                .email("test@mail.com")
                .build();

        when(userService.registerUser(any(SaveUserRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@mail.com"));

        verify(userService).registerUser(any(SaveUserRequest.class));
    }

    @Test
    void login_shouldReturnToken() throws Exception {
        LoginUserRequest request = new LoginUserRequest();
        request.setEmail("john@mail.com");
        request.setPassword("123");

        UserAuthResponse response = UserAuthResponse.builder()
                .token("jwt-token")
                .userId(1L)
                .name("John")
                .surname("Doe")
                .build();

        when(userService.login(any())).thenReturn(response);

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void update_whenAuthenticated_shouldReturnUpdatedUser() throws Exception {
        authenticate(testUser);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Updated");

        UserDto dto = UserDto.builder()
                .id(1L)
                .name("Updated")
                .build();

        when(userService.updateUser(eq(testUser.getId()), any(UpdateUserRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void update_whenNotAuthenticated_shouldReturn401() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_whenAdmin_shouldReturnNoContent() throws Exception {
        testUser.setUserType(UserType.ADMIN);
        authenticate(testUser);

        doNothing().when(userService).delete(eq(2L), any(User.class));

        mockMvc.perform(delete("/users/2"))
                .andExpect(status().isNoContent());

        verify(userService).delete(eq(2L), any(User.class));
    }

    @Test
    void delete_whenNotAdmin_shouldReturnForbidden() throws Exception {
        authenticate(testUser);

        doThrow(new AccessDeniedException("Only admin"))
                .when(userService).delete(eq(2L), any(User.class));

        mockMvc.perform(delete("/users/2"))
                .andExpect(status().isForbidden());
    }
}