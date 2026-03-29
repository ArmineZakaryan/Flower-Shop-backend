package org.example.flowershop.endpoint;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowershop.dto.LoginUserRequest;
import org.example.flowershop.dto.SaveUserRequest;
import org.example.flowershop.dto.UpdateUserRequest;
import org.example.flowershop.dto.UserAuthResponse;
import org.example.flowershop.dto.UserDto;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Slf4j
public class UserEndpoint {

    private final UserService userService;

    @GetMapping
    public List<UserDto> getAll(@RequestParam(defaultValue = "") String sort) {

        log.info("GET /users called with sort={}", sort);

        List<UserDto> users = userService.getAllUsers(sort);

        log.info("GET /users returned {} users", users.size());

        return users;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable long id) {

        log.info("GET /users/{} called", id);

        UserDto user = userService.getUserById(id);

        log.info("User fetched successfully id={}", id);

        return ResponseEntity.ok(user);
    }

    @PutMapping
    public ResponseEntity<UserDto> update(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        log.info("PUT /users called by userId={}", currentUser.getId());
        log.debug("Update request: {}", request);

        UserDto updatedUser = userService.updateUser(currentUser.getId(), request);

        log.info("User updated successfully id={}", currentUser.getId());

        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody SaveUserRequest request) {

        log.info("POST /users/register called email={} username={}",
                request.getEmail(), request.getUsername());

        UserDto user = userService.registerUser(request);

        log.info("User registered successfully id={}", user.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<UserAuthResponse> login(@Valid @RequestBody LoginUserRequest request) {

        log.info("POST /users/login attempt email={}", request.getEmail());

        UserAuthResponse response = userService.login(request);

        log.info("Login successful for userId={}", response.getUserId());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable long id,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        log.info("DELETE /users/{} called by userId={}", id, currentUser.getId());

        userService.delete(id, currentUser);

        log.info("User deleted successfully id={}", id);

        return ResponseEntity.noContent().build();
    }
}