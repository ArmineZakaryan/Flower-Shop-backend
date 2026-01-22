package org.example.flowershop.security;

import lombok.Getter;
import org.example.flowershop.model.entity.User;
import org.springframework.security.core.authority.AuthorityUtils;

@Getter
public class CurrentUser extends org.springframework.security.core.userdetails.User {
    private final User user;

    public CurrentUser(User user) {
        super(
                user.getEmail(),
                user.getPassword(),
                AuthorityUtils.createAuthorityList(
                        user.getUserType() != null ? user.getUserType().name() : "USER"
                )
        );
        this.user = user;
    }

}