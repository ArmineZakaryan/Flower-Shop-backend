package org.example.flowershop.security;

import lombok.RequiredArgsConstructor;
import org.example.flowershop.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userService.findByEmail(username)
                .map(CurrentUser::new)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User with " + username + " email does not exist"
                        ));
    }
}