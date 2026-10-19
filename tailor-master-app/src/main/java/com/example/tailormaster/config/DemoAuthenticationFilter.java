package com.example.tailormaster.config;

import com.example.tailormaster.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Profile("demo")
public class DemoAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public DemoAuthenticationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

    }

    // Baqi doFilterInternal() method bilkul waisa hi rahega.
}