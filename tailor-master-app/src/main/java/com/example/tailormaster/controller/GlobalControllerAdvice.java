package com.example.tailormaster.controller;

import com.example.tailormaster.entity.User;
import com.example.tailormaster.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserService userService;

    public GlobalControllerAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute
    public void addAttributes(Model model, HttpServletRequest request) {

        String uri = request.getRequestURI();

        if (uri.contains("/customers")) {

            model.addAttribute("activePage", "customers");

        } else if (uri.contains("/orders/pending-payments")) {

            model.addAttribute("activePage", "orders/pending-payments");

        } else if (uri.contains("/orders")) {

            model.addAttribute("activePage", "orders");

        } else if (uri.contains("/products")) {

            model.addAttribute("activePage", "products");

        } else if (uri.contains("/inventory")) {

            // Inventory ke tamam pages ke liye
            model.addAttribute("activePage", "inventory");

        } else if (uri.contains("/dashboard")) {

            model.addAttribute("activePage", "dashboard");

        } else if (uri.contains("/users/profile")) {

            model.addAttribute("activePage", "profile");

        } else if (uri.contains("/labors")) {

            model.addAttribute("activePage", "labors");
        } else if (uri.contains("/inventory/colors")) {

            model.addAttribute("activePage", "colors");
        } else if (uri.contains("/inventory/designs")) {

            model.addAttribute("activePage", "designs");
        } else if (uri.contains("/backup/settings")) {
            model.addAttribute("activePage", "settings");
        } else if (uri.contains("/backup/history")) {
            model.addAttribute("activePage", "history");
        }


        // Get logged-in user and add shop name or whole user to model
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {

            String username = auth.getName();

            Optional<User> user = userService.findByUsername(username);

            user.ifPresent(tempUser -> {

                // Add shop name to model
                model.addAttribute("shopName", tempUser.getShopName());
                model.addAttribute("fullName", tempUser.getFullName());

                // Trial message logic
                LocalDate today = LocalDate.now();
                LocalDate trialEnd = tempUser.getTrialEndsAt();

                if (trialEnd != null) {

                    long daysRemaining =
                            ChronoUnit.DAYS.between(today, trialEnd);

                    if (daysRemaining >= 0 && daysRemaining <= 5) {

                        model.addAttribute(
                                "trialMessage",
                                "Your free trial will expire in "
                                        + daysRemaining
                                        + " day(s). Please contact us to continue using the application."
                        );
                    }
                }
            });
        }
    }
}

