package com.example.tailormaster.config;

import com.example.tailormaster.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Profile("demo")
public class DemoAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public DemoAuthenticationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("Before filterChain: status={}, URI={}",
                response.getStatus(), request.getRequestURI());

        filterChain.doFilter(request, response);

        log.info("After filterChain: status={}, URI={}",
                response.getStatus(), request.getRequestURI());

        log.info("DemoAuthenticationFilter called for: {}", request.getRequestURI());

        log.info("Before filterChain: status={}, URI={}",
                response.getStatus(), request.getRequestURI());

        filterChain.doFilter(request, response);

        log.info("After filterChain: status={}, URI={}",
                response.getStatus(), request.getRequestURI());
    }

    // Baqi doFilterInternal() method bilkul waisa hi rahega.
}