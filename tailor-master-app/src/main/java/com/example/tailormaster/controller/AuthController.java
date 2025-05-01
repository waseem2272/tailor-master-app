package com.example.tailormaster.controller;

import com.example.tailormaster.entity.User;
import com.example.tailormaster.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@AllArgsConstructor
@Controller
public class AuthController {

    private static final Logger logger = LogManager.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Serve the login page
    @GetMapping("/login")
    public String showLoginPage() {
        logger.info("Accessed login page");
        return "login";
    }

    // This method will be called on successful login
    @GetMapping("/dashboard")
    public String showHomePage(Model model, Authentication authentication) {
        String username = authentication.getName();
        logger.info("User '{}' accessed the dashboard", username);
        model.addAttribute("username", username);
        return "dashboard";
    }

    // Handle logout
    @RequestMapping("/logout")
    public String logoutPage() {
        logger.info("Logout endpoint hit — redirecting to login with logout param");
        return "redirect:/login?logout";
    }

    // Optional: Customize the logout success handler
    @GetMapping("/logout-success")
    public String logoutSuccessPage(Model model) {
        logger.info("User successfully logged out — showing login page with message");
        model.addAttribute("message", "You have been logged out successfully.");
        return "login";
    }

    @PostMapping("/update-password")
    public String updatePassword(@RequestParam String username,
                                 @RequestParam String newPassword,
                                 RedirectAttributes redirectAttributes) {
        logger.info("Attempting to update password for user: {}", username);
        try {
            Optional<User> user = userRepository.findByUsername(username);
            if (user.isEmpty()) {
                logger.warn("Username not found during password update: {}", username);
                redirectAttributes.addFlashAttribute("error", "Username not found.");
                return "redirect:/login";
            }

            User user1 = user.get();
            logger.debug("Found user with username: {}", username);

            // Encode the new password using Spring Security's PasswordEncoder
            String encodedPassword = passwordEncoder.encode(newPassword);
            user1.setPassword(encodedPassword);
            userRepository.save(user1);
            logger.info("Password updated successfully for user: {}", username);
            redirectAttributes.addFlashAttribute("success", "Password updated successfully.");

        } catch (Exception e) {
            logger.error("An error occurred while updating the password for user {}: {}", username, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "An error occurred while updating the password.");
        }
        return "redirect:/login";
    }
}
