package org.example.flowershop.security;

import lombok.RequiredArgsConstructor;
import org.example.flowershop.model.entity.User;
import org.example.flowershop.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> byEmail = userService.findByEmail(username);
        if (byEmail.isPresent()) {
            return new CurrentUser(byEmail.get());
        }

        throw new UsernameNotFoundException("User with " + username + " email does not exists");
    }
}