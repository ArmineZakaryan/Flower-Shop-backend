package org.example.flowershop.mapper;

import org.example.flowershop.dto.SaveUserRequest;
import org.example.flowershop.dto.UserDto;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.model.enums.UserType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    @Mapping(target = "userType", source = "userType")
    User toEntity(SaveUserRequest request);

    default UserType mapUserType(String userType) {
        if (userType == null || userType.isBlank()) {
            return UserType.USER;
        }
        return UserType.valueOf(userType.toUpperCase());
    }
}
