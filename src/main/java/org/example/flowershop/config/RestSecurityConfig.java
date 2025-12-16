package org.example.flowershop.config;

import lombok.RequiredArgsConstructor;
import org.example.flowershop.filter.JwtAuthenticationTokenFilter;
import org.example.flowershop.security.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class RestSecurityConfig {

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
        httpSecurity
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(authenticationEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize


                        .requestMatchers(HttpMethod.POST, "/users/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/login").permitAll()

                        .requestMatchers(HttpMethod.GET, "/categories").permitAll()
                        .requestMatchers("/categories/**").hasAuthority("ADMIN")


                        .requestMatchers(HttpMethod.GET, "/favorites/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/orders/**").authenticated()
                        .requestMatchers("/cartItem/**").authenticated()

                        .requestMatchers(HttpMethod.GET, "/products").permitAll()
                        .requestMatchers("/products/**").hasAuthority("ADMIN")

                        .anyRequest().permitAll()
                );
        httpSecurity.addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }
}