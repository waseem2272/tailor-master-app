
package com.example.tailormaster.config;

import com.example.tailormaster.entity.User;
import com.example.tailormaster.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Slf4j
@Profile("demo")
public class DemoAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public DemoAuthenticationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String username = System.getenv("DEMO_USERNAME");

        if (username == null || username.isBlank()) {
            response.sendError(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Demo user is not configured."
            );
            return;
        }

        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null || !user.isEnabled()) {
            response.sendError(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Demo user is unavailable."
            );
            return;
        }

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());

        var authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                authorities
        );

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Login page ki jagah dashboard kholna hai.
        if ("/login".equals(request.getServletPath())
                && "GET".equalsIgnoreCase(request.getMethod())) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
