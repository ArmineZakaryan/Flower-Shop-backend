package org.example.flowershop.endpoint;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.example.flowershop.repository.UserRepository;
import org.example.flowershop.security.CurrentUser;
import org.example.flowershop.service.UserService;
import org.example.flowershop.util.JwtTokenUtil;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Slf4j
public class UserEndpoint {
    private final UserRepository userRepository;
    private final UserService userService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil tokenUtil;


    @GetMapping
    public List<UserDto> getAllUsers(
            @RequestHeader(required = false, name = "x-auth-token")
            @RequestParam(required = false, defaultValue = "") String sort) {
        Set<String> allowed = Set.of("username", "email", "name");

        if (!allowed.contains(sort)) {
            sort = "username";
        }
        log.info("Fetching all users, sorted by {}", sort);
        return userRepository.findAll(Sort.by(sort))
                .stream()
                .map(userMapper::toDto)
                .toList();
    }


    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable long id) {
        log.info("Fetching user with id: {}", id);

        var user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User with id {} not found", id);
                    return new UserNotFoundException("User with id " + id + " not found");
                });

        log.info("User found with id: {}", id);
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@RequestBody SaveUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.error("User creation failed: Email already exists {}", request.getEmail());
            throw new EmailAlreadyExistsException("Email already exists!");
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.error("User creation failed: Username already exists {}", request.getUsername());
            throw new UsernameAlreadyExistsException("Username already exists!");
        }

        request.setPassword(passwordEncoder.encode(request.getPassword()));

        var user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);

        log.info("User created successfully: {}", savedUser.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toDto(savedUser));
    }

    @PutMapping
    public ResponseEntity<UserDto> update(
            @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal CurrentUser currentUser) {

        if (currentUser == null) {
            log.error("User update failed: User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = currentUser.getUser();
        if (user == null) {
            log.error("User update failed: User object is null in CurrentUser");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = user.getId();
        log.info("Updating user with id: {}", userId);

        UserDto updatedUser = userService.updateUser(userId, request);

        log.info("User updated successfully: {}", updatedUser.getEmail());
        return ResponseEntity.ok(updatedUser);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id,
                                       @AuthenticationPrincipal(expression = "user") User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        var user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User deletion failed: User with id {} not found", id);
                    return new UserNotFoundException("User with id " + id + " not found");
                });

        if (currentUser.getId() != id && currentUser.getUserType() != UserType.ADMIN) {
            log.error("User deletion failed: Insufficient permissions to delete user with id {}", id);

            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        userRepository.delete(user);
        log.info("User deleted successfully: {}", user.getEmail());
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/register")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "409", description = "Email already exists"),
            @ApiResponse(responseCode = "200", description = "Registration is success")
    })
    public ResponseEntity<UserDto> register(@RequestBody SaveUserRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        if (userService.findByEmail(request.getEmail()).isPresent()) {
            log.error("Registration failed: Email {} already exists", request.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userService.save(user);

        log.info("User registered successfully: {}", user.getEmail());
        return ResponseEntity.ok().build();
    }


    @PostMapping("/login")
    public ResponseEntity<UserAuthResponse> login(@RequestBody LoginUserRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        Optional<User> byEmail = userService.findByEmail(request.getEmail());
        if (byEmail.isEmpty()) {
            log.warn("Login failed: Invalid email or password for {}", request.getEmail());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        User user = byEmail.get();
        if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.info("Login successful for user: {}", user.getEmail());
            return ResponseEntity
                    .ok(UserAuthResponse.builder()
                            .token(tokenUtil.generateToken(user.getEmail()))
                            .name(user.getName())
                            .surname(user.getSurname())
                            .userId(String.valueOf(user.getId()))
                            .username(user.getUsername())
                            .build());
        }

        log.warn("Login failed: Incorrect password for user {}", request.getEmail());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .build();
    }
}