package com.example.tailormaster.controller;

import com.example.tailormaster.entity.InventoryBrand;
import com.example.tailormaster.service.InventoryBrandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/inventory/brands")
@RequiredArgsConstructor
@Log4j2
public class InventoryBrandController {

    private final InventoryBrandService inventoryBrandService;

    // =========================
    // Brand List
    // =========================
    @GetMapping
    public String list(Model model) {
        try {
            log.info("Loading inventory brand list");
            model.addAttribute("brands", inventoryBrandService.getAll());
            model.addAttribute("brand", new InventoryBrand());
            return "inventory/brands";
        } catch (Exception e) {
            log.error("Error while loading inventory brand list", e);
            model.addAttribute("errorMessage", "Unable to load brands.");
            return "error";
        }
    }

    // =========================
    // Save Brand
    // =========================
    @PostMapping("/save")
    public String save(@ModelAttribute("brand") InventoryBrand brand) {
        try {
            log.info("Saving inventory brand. Name: {}", brand.getName());
            inventoryBrandService.save(brand);
            log.info("Inventory brand saved successfully. Brand ID: {}", brand.getId());
            return "redirect:/inventory/brands";
        } catch (Exception e) {
            log.error("Error while saving inventory brand. Name: {}", brand.getName(), e);
            return "redirect:/inventory/brands?error=save";
        }
    }

    // =========================
    // Delete Brand
    // =========================
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        try {
            log.info("Deleting inventory brand. Brand ID: {}", id);
            inventoryBrandService.delete(id);
            log.info("Inventory brand deleted successfully. Brand ID: {}", id);
            return "redirect:/inventory/brands";
        } catch (Exception e) {
            log.error("Error while deleting inventory brand. Brand ID: {}", id, e);
            return "redirect:/inventory/brands?error=delete";
        }
    }
}