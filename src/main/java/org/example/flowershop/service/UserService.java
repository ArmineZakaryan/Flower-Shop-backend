package org.example.flowershop.service;

import org.example.flowershop.dto.LoginUserRequest;
import org.example.flowershop.dto.UpdateUserRequest;
import org.example.flowershop.dto.UserDto;
import org.example.flowershop.model.entity.User;

import java.util.Optional;

public interface UserService {
    User registerUser(User user);


    UserDto updateUser(Long id, UpdateUserRequest request);


    User login(LoginUserRequest request);


    Optional<User> findByEmail(String email);

    User save(User user);

}