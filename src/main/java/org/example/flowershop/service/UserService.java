package org.example.flowershop.service;

import org.example.flowershop.dto.LoginUserRequest;
import org.example.flowershop.dto.SaveUserRequest;
import org.example.flowershop.dto.UpdateUserRequest;
import org.example.flowershop.dto.UserAuthResponse;
import org.example.flowershop.dto.UserDto;
import org.example.flowershop.model.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    UserDto registerUser(SaveUserRequest request);

    UserDto updateUser(long id, UpdateUserRequest request);

    UserAuthResponse login(LoginUserRequest request);

    List<UserDto> getAllUsers(String sort);

    UserDto getUserById(long id);

    Optional<User> findByEmail(String email);

    void delete(long id, User currentUser);
}