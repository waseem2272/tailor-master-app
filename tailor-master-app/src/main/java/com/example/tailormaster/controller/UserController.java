package com.example.tailormaster.controller;

import com.example.tailormaster.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class UserController {

    private UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/admin/staff")
    public String viewStaff(Model model) {
        model.addAttribute("staff", userRepository.findAll());
        return "admin/staff";
    }

}
