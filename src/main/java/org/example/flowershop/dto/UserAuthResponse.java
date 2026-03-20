package org.example.flowershop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.flowershop.model.enums.UserType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAuthResponse {
    private String token;
    private String name;
    private String surname;
    private String username;
    private String email;
    private UserType userType;
    private Long userId;
}