package com.example.tailormaster.controller;

import com.example.tailormaster.dto.RegisterUserForm;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegisterController {
    private final UserService userService;

    public RegisterController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("user", new RegisterUserForm());
        model.addAttribute("roles", userService.getRoles()); // Provide a list of available roles
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") RegisterUserForm registerUserForm,
                               BindingResult result,
                               RedirectAttributes redirectAttributes, Model model) {

        if (result.hasErrors()) {
            model.addAttribute("roles", userService.getRoles()); // Reload roles for the form
            return "register";
        }

        try {
            User user = populateUser(registerUserForm);

            String selectedRole = registerUserForm.getRole(); // Get the selected role from the form

            // Save the user details
            userService.registerUser(user, selectedRole); // Call the updated registerUser method

            // Send SMS to the staff with login credentials
//        if ("STAFF".equals(user.getRole())) {
//            smsService.sendLoginDetails(user.getPhone(), user.getUsername(), user.getPassword());
//        }

            // Redirect to login page with a success message
            redirectAttributes.addFlashAttribute("success", "Registration successful. You can now log in.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            e.printStackTrace();
            redirectAttributes.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
    }

    private User populateUser(RegisterUserForm registerUserForm) {
        // populate the user
        User user = new User();
        user.setUsername(registerUserForm.getUsername());
        user.setPassword(registerUserForm.getPassword());
        user.setFullName(registerUserForm.getFullName());
        user.setFathersName(registerUserForm.getFathersName());
        user.setPhone1(registerUserForm.getPhone1());
        user.setPhone2(registerUserForm.getPhone2());
        user.setDateOfBirth(registerUserForm.getDateOfBirth());
        user.setShopName(registerUserForm.getShopName());
        user.setProprietorName(registerUserForm.getProprietorName());
        user.setShopAddress(registerUserForm.getShopAddress());
        user.setShortCode(registerUserForm.getShortCode());
        return user;
    }
}
