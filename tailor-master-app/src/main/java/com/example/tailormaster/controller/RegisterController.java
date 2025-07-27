package com.example.tailormaster.controller;

import com.example.tailormaster.dto.RegisterUserForm;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.service.UserService;
import com.example.tailormaster.validation.Utility;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

@Controller
@RequestMapping("/users")
public class RegisterController {

    private static final Logger logger = LogManager.getLogger(RegisterController.class);

    private final UserService userService;

    public RegisterController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        logger.info("Displaying registration page.");
        try {
            model.addAttribute("user", new RegisterUserForm());
            model.addAttribute("roles", userService.getRoles()); // Provide a list of available roles
            logger.debug("Available roles: {}", userService.getRoles());
            return "register";
        } catch (Exception e) {
            logger.error("Error while displaying registration page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while loading the registration page.");
            return "login"; // Or a specific error page
        }
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") RegisterUserForm registerUserForm,
                               BindingResult result,
                               @RequestParam("logo") MultipartFile logoFile,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        logger.info("Attempting to register user with data: {}", registerUserForm);

        if (result.hasErrors()) {
            logger.warn("Validation errors during registration: {}", result.getAllErrors());
            model.addAttribute("roles", userService.getRoles());
            return "register";
        }

        try {
            User user = populateUser(registerUserForm);
            logger.debug("Populated user object: {}", user);
            // Handle image file (PNG, JPG, JPEG)
            if (!logoFile.isEmpty()) {
                String contentType = logoFile.getContentType();
                logger.info("Uploaded logo file: name={}, size={}, contentType={}", logoFile.getOriginalFilename(), logoFile.getSize(), contentType);
                if (!("image/png".equals(contentType) || "image/jpeg".equals(contentType) || "image/jpg".equals(contentType))) {
                    logger.warn("Invalid logo file type: {}", contentType);
                    model.addAttribute("roles", userService.getRoles());
                    result.rejectValue("logo", "error.user", "Only PNG, JPG, or JPEG files are allowed.");
                    return "register";
                }
                user.setLogo(logoFile.getBytes());
                user.setLogoContentType(contentType);
                logger.debug("Logo set for user.");
            }

//            String selectedRole = registerUserForm.getRole();
            String selectedRole = "ROLE_ADMIN";
            userService.registerUser(user, selectedRole);
            logger.info("User registration successful for username: {}", user.getUsername());
            redirectAttributes.addFlashAttribute("success", "Registration successful. You can now log in.");
            return "redirect:/login";
        } catch (IOException e) {
            logger.error("IO Exception while processing logo for user {}: {}", registerUserForm.getUsername(), e.getMessage(), e);
            result.reject("logo", "Failed to process the uploaded logo.");
            model.addAttribute("roles", userService.getRoles());
            return "register";
        } catch (RuntimeException e) {
            logger.error("Runtime Exception during registration for user {}: {}", registerUserForm.getUsername(), e.getMessage(), e);
            redirectAttributes.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
    }
    @GetMapping("/logo/{username}")
    @ResponseBody
    public ResponseEntity<byte[]> getUserLogo(@PathVariable String username) {
        logger.info("Fetching logo for user: {}", username);
        try {
            Optional<User> userOptional = userService.findByUsername(username);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                if (user.getLogo() != null) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.parseMediaType(user.getLogoContentType()));
                    logger.debug("Logo found for user: {}, contentType: {}", username, user.getLogoContentType());
                    return new ResponseEntity<>(user.getLogo(), headers, HttpStatus.OK);
                } else {
                    logger.debug("No logo found for user: {}", username);
                    return ResponseEntity.notFound().build();
                }
            } else {
                logger.warn("User not found while fetching logo: {}", username);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error fetching logo for user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    private User populateUser(RegisterUserForm registerUserForm) {
        logger.debug("Populating user object from RegisterUserForm: {}", registerUserForm);
        // populate the user
        User user = new User();
        user.setUsername(registerUserForm.getUsername());
        user.setPassword(registerUserForm.getPassword());
        user.setFullName(registerUserForm.getFullName());
//        user.setFathersName(registerUserForm.getFathersName());
        user.setPhone1(registerUserForm.getPhone1());
        user.setPhone2(registerUserForm.getPhone2());
//        user.setDateOfBirth(registerUserForm.getDateOfBirth());
        user.setShopName(registerUserForm.getShopName());
        user.setProprietorName(registerUserForm.getProprietorName());
        user.setShopAddress(registerUserForm.getShopAddress());

        user.setTrialStartedAt(LocalDate.now());
        user.setTrialEndsAt(LocalDate.now().plusMonths(1));

        // get shortcode
        String shortCode = Utility.generateShortCode(registerUserForm.getShopName());

        user.setShortCode(shortCode);
        return user;
    }

    @GetMapping("/profile")
    public String showUserProfile(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        logger.info("Displaying user profile: {}", principal.getName());
        try {
            Optional<User> userOptional = userService.findByUsername(principal.getName());
            if (userOptional.isEmpty()) {
                logger.warn("User not found: {}", principal.getName());
                redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
                return "redirect:/dashboard";
            }
            model.addAttribute("user", userOptional.get());
            logger.info("User profile data: {}", userOptional.get());
            return "user-profile";
        } catch (Exception e) {
            logger.error("Error displaying user profile {}: {}", principal.getName(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while loading your profile.");
            return "redirect:/dashboard"; // Or a specific error page
        }
    }

    @GetMapping("/profile/edit")
    public String editUserProfile(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        logger.info("Displaying edit user profile page for: {}", principal.getName());
        try {
            Optional<User> userOptional = userService.findByUsername(principal.getName());
            if (userOptional.isEmpty()) {
                logger.warn("User not found: {}", principal.getName());
                redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
                return "redirect:/dashboard";
            }
            model.addAttribute("user", userOptional.get());
            logger.debug("User data for edit profile: {}", userOptional.get());
            return "edit-profile";
        } catch (Exception e) {
            logger.error("Error displaying edit user profile page {}: {}", principal.getName(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while loading the edit profile page.");
            return "redirect:/dashboard"; // Or a specific error page
        }
    }

    // Handle profile update
    @PostMapping("/profile/update")
    public String updateProfile(
            @Valid @ModelAttribute("user") User user,
            BindingResult result,
            @RequestParam("logoFile") MultipartFile logoFile,
            RedirectAttributes redirectAttributes,
            Model model) {

        logger.info("Attempting to update user profile for user: {}", user);

        // Validate file if uploaded
        if (!logoFile.isEmpty()) {
            String contentType = logoFile.getContentType();
            if (!("image/png".equals(contentType) || "image/jpeg".equals(contentType) || "image/jpg".equals(contentType))) {
                result.rejectValue("logo", "error.user", "Only PNG, JPG, or JPEG files are allowed.");
            } else {
                try {
                    user.setLogo(logoFile.getBytes());
                    user.setLogoContentType(contentType);
                } catch (IOException e) {
                    logger.error("Error reading uploaded logo for user {}: {}", user.getUsername(), e.getMessage());
                    result.rejectValue("logo", "error.user", "Failed to read the uploaded logo.");
                }
            }
        }

        if (result.hasErrors()) {
            logger.warn("Validation errors during profile update: {}", result.getAllErrors());
            model.addAttribute("user", user);
            return "edit-profile"; // Make sure this is the correct Thymeleaf view name
        }

        try {
            userService.updateUser(user);
            logger.info("User profile updated successfully for user: {}", user);
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
            return "redirect:/users/profile";
        } catch (Exception e) {
            logger.error("Error updating user profile for user {}: {}", user.getUsername(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "An error occurred while updating the profile. Please try again.");
            return "redirect:/users/profile/edit";
        }
    }

}
