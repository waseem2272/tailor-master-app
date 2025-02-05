package com.example.tailormaster.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
public class StaffController {
    @GetMapping("/dashboard")
    public String staffDashboard(Model model) {
        // Fetch necessary staff-specific data here
        return "staff/dashboard";
    }
}
