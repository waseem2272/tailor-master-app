package com.example.tailormaster.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {

    // Serve the login page
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";  // points to login.html in the templates folder
    }

    // This method will be called on successful login (Spring Security handles the redirect)
    @GetMapping("/dashboard")
    public String showHomePage(Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());  // You can customize this to show user details
        return "dashboard";  // points to dashboard.html (dashboard or landing page)
    }

    // Handle logout - Redirect user to login page after logout
    @RequestMapping("/logout")
    public String logoutPage() {
        return "redirect:/login?logout";  // Spring Security handles the logout and redirects to the login page
    }

    // Optional: Customize the logout success handler
    @GetMapping("/logout-success")
    public String logoutSuccessPage(Model model) {
        model.addAttribute("message", "You have been logged out successfully.");
        return "login";  // Show login page with a logout success message
    }
}
