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
        return userService.getAllUsers(sort);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping
    public ResponseEntity<UserDto> update(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        return ResponseEntity.ok(
                userService.updateUser(currentUser.getId(), request)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody SaveUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.registerUser(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UserAuthResponse> login(@Valid @RequestBody LoginUserRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable long id,
            @AuthenticationPrincipal(expression = "user") User currentUser) {

        userService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}