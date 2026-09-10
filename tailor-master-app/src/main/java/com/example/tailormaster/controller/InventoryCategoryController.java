package com.example.tailormaster.controller;

import com.example.tailormaster.entity.InventoryCategory;
import com.example.tailormaster.enums.ItemType;
import com.example.tailormaster.service.InventoryCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/inventory/categories")
@RequiredArgsConstructor
@Log4j2
public class InventoryCategoryController {

    private final InventoryCategoryService inventoryCategoryService;

    // =========================
    // Category List
    // =========================
    @GetMapping
    public String list(Model model) {
        try {
            log.info("Loading inventory category list");
            model.addAttribute("categories", inventoryCategoryService.getAllActive());
            model.addAttribute("category", new InventoryCategory());
            model.addAttribute("itemTypes", ItemType.values());
            return "inventory/categories";
        } catch (Exception e) {
            log.error("Error while loading inventory category list", e);
            model.addAttribute("errorMessage", "Unable to load categories.");
            return "error";
        }
    }

    // =========================
    // Save Category
    // =========================
    @PostMapping("/save")
    public String save(@ModelAttribute("category") InventoryCategory category) {
        try {
            log.info("Saving inventory category. Name: {}, Type: {}",
                    category.getName(), category.getItemType());
            inventoryCategoryService.save(category);
            log.info("Inventory category saved successfully. Category ID: {}", category.getId());
            return "redirect:/inventory/categories";
        } catch (Exception e) {
            log.error("Error while saving inventory category. Name: {}", category.getName(), e);
            return "redirect:/inventory/categories?error=save";
        }
    }

    // =========================
    // Delete Category
    // =========================
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        try {
            log.info("Deleting inventory category. Category ID: {}", id);
            inventoryCategoryService.delete(id);
            log.info("Inventory category deleted successfully. Category ID: {}", id);
            return "redirect:/inventory/categories";
        } catch (Exception e) {
            log.error("Error while deleting inventory category. Category ID: {}", id, e);
            return "redirect:/inventory/categories?error=delete";
        }
    }
}