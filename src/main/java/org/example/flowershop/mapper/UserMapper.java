package org.example.flowershop.mapper;

import org.example.flowershop.dto.SaveUserRequest;
import org.example.flowershop.dto.UserDto;
import org.example.flowershop.model.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);

    User toEntity(SaveUserRequest request);
}
