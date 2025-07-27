package com.example.tailormaster.config;

import com.example.tailormaster.entity.User;
import com.example.tailormaster.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

@Component
@AllArgsConstructor
public class TrialCheckSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository; // or UserService

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<User> user = userRepository.findByUsername(userDetails.getUsername());

        LocalDate today = LocalDate.now();
        if (user.isPresent() && user.get().getTrialEndsAt() != null && today.isAfter(user.get().getTrialEndsAt())) {
            response.sendRedirect(request.getContextPath() + "/login?trial=expired");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/dashboard");
    }
}
