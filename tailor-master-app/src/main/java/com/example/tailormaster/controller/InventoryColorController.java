package com.example.tailormaster.controller;

import com.example.tailormaster.entity.InventoryColor;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.service.InventoryColorService;
import com.example.tailormaster.util.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventory/colors")
@RequiredArgsConstructor
@Slf4j
public class InventoryColorController {

    private final InventoryColorService inventoryColorService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping
    public String listColors(Model model) {
        User user = getCurrentUser();
        model.addAttribute("colors", inventoryColorService.getAllColors(user));
        model.addAttribute("activePage", "inventory/colors");
        return "inventory/colors";
    }

    @GetMapping("/add")
    public String addColorForm(Model model) {
        model.addAttribute("color", new InventoryColor());
        model.addAttribute("activePage", "inventory/colors");
        return "inventory/color-form";
    }

    @PostMapping("/save")
    public String saveColor(@ModelAttribute("color") InventoryColor color,
                            RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();
            inventoryColorService.save(user, color);
            redirectAttributes.addFlashAttribute("successMessage", "Color saved successfully.");
        } catch (IllegalArgumentException e) {
            log.warn("Unable to save inventory color: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error while saving inventory color", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to save color.");
        }
        return "redirect:/inventory/colors";
    }

    @GetMapping("/edit/{id}")
    public String editColor(@PathVariable Long id,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();

            InventoryColor color = inventoryColorService.findById(user, id)
                    .orElseThrow(() -> new IllegalArgumentException("Color not found."));

            model.addAttribute("color", color);
            return "inventory/color-form";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/inventory/colors";
        } catch (Exception e) {
            log.error("Error while loading inventory color. ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to load color.");
            return "redirect:/inventory/colors";
        }
    }

    @PostMapping("/deactivate/{id}")
    public String deactivateColor(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();
            inventoryColorService.deactivate(user, id);
            redirectAttributes.addFlashAttribute("successMessage", "Color deactivated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error while deactivating inventory color. ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to deactivate color.");
        }
        return "redirect:/inventory/colors";
    }

    @PostMapping("/activate/{id}")
    public String activateColor(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();
            inventoryColorService.activate(user, id);
            redirectAttributes.addFlashAttribute("successMessage", "Color activated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error while activating inventory color. ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to activate color.");
        }
        return "redirect:/inventory/colors";
    }

    private User getCurrentUser() {
        return authenticatedUserService.getCurrentUser();
    }
}