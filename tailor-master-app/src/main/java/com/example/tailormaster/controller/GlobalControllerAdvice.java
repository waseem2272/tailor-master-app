package com.example.tailormaster.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

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
        } else if (uri.contains("/dashboard")) {
            model.addAttribute("activePage", "dashboard");
        } else if (uri.contains("/users/profile")) {
            model.addAttribute("activePage", "profile");
        }
    }
}
