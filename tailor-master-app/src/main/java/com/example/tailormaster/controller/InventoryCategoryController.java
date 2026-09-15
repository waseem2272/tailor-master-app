package com.example.tailormaster.controller;

import com.example.tailormaster.entity.InventoryCategory;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.enums.ItemType;
import com.example.tailormaster.service.InventoryCategoryService;
import com.example.tailormaster.util.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventory/categories")
@RequiredArgsConstructor
@Slf4j
public class InventoryCategoryController {

    private final InventoryCategoryService inventoryCategoryService;
    private final AuthenticatedUserService authenticatedUserService;

    private User getCurrentUser() {
        return authenticatedUserService.getCurrentUser();
    }

    @GetMapping
    public String listCategories(Model model) {
        try {
            User user = getCurrentUser();

            model.addAttribute("categories", inventoryCategoryService.getAllCategories(user));
            model.addAttribute("activePage", "inventory/categories");

            return "inventory/categories";
        } catch (Exception e) {
            log.error("Error while loading inventory categories", e);
            model.addAttribute("errorMessage", "Unable to load categories.");
            return "inventory/categories";
        }
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("category", new InventoryCategory());
        model.addAttribute("itemTypes", ItemType.values());
        model.addAttribute("activePage", "inventory/categories");

        return "inventory/category-form";
    }

    @PostMapping("/save")
    public String saveCategory(@ModelAttribute("category") InventoryCategory category,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();

            inventoryCategoryService.save(user, category);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Category saved successfully.");
        } catch (IllegalArgumentException e) {
            log.warn("Unable to save inventory category: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error while saving inventory category", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Unable to save category.");
        }

        return "redirect:/inventory/categories";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();

            InventoryCategory category =
                    inventoryCategoryService.getById(user, id);

            model.addAttribute("category", category);
            model.addAttribute("itemTypes", ItemType.values());
            model.addAttribute("activePage", "inventory/categories");

            return "inventory/category-form";
        } catch (IllegalArgumentException e) {
            log.warn("Category not found. ID: {}, Message: {}",
                    id, e.getMessage());

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());

            return "redirect:/inventory/categories";
        } catch (Exception e) {
            log.error("Error while loading inventory category. ID: {}",
                    id, e);

            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Unable to load category.");

            return "redirect:/inventory/categories";
        }
    }

    @PostMapping("/deactivate/{id}")
    public String deactivateCategory(@PathVariable Long id,
                                     RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();

            inventoryCategoryService.deactivate(user, id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Category deactivated successfully.");
        } catch (IllegalArgumentException e) {
            log.warn("Unable to deactivate category. ID: {}, Message: {}",
                    id, e.getMessage());

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error while deactivating inventory category. ID: {}",
                    id, e);

            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Unable to deactivate category.");
        }

        return "redirect:/inventory/categories";
    }

    @PostMapping("/activate/{id}")
    public String activateCategory(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();

            inventoryCategoryService.activate(user, id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Category activated successfully.");
        } catch (IllegalArgumentException e) {
            log.warn("Unable to activate category. ID: {}, Message: {}",
                    id, e.getMessage());

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error while activating inventory category. ID: {}",
                    id, e);

            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Unable to activate category.");
        }

        return "redirect:/inventory/categories";
    }
}