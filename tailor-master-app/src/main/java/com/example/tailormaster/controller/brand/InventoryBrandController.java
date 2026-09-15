package com.example.tailormaster.controller.brand;

import com.example.tailormaster.entity.InventoryBrand;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.service.InventoryBrandService;
import com.example.tailormaster.util.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventory/brands")
@RequiredArgsConstructor
@Slf4j
public class InventoryBrandController {

    private final InventoryBrandService inventoryBrandService;
    private final AuthenticatedUserService authenticatedUserService;

    private User getCurrentUser() {
        return authenticatedUserService.getCurrentUser();
    }

    @GetMapping
    public String listBrands(Model model) {
        try {
            User user = getCurrentUser();

            model.addAttribute("brands", inventoryBrandService.getAllBrands(user));
            model.addAttribute("activePage", "inventory/brands");

            return "inventory/brands";

        } catch (Exception e) {
            log.error("Error while loading inventory brands", e);
            model.addAttribute("errorMessage", "Unable to load brands.");
            return "inventory/brands";
        }
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("brand", new InventoryBrand());
        model.addAttribute("activePage", "inventory/brands");
        return "inventory/brand-form";
    }

    @PostMapping("/save")
    public String saveBrand(@ModelAttribute("brand") InventoryBrand brand,
                            RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();

            inventoryBrandService.save(user, brand);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Brand saved successfully.");

        } catch (IllegalArgumentException e) {
            log.warn("Unable to save inventory brand: {}", e.getMessage());
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());

        } catch (Exception e) {
            log.error("Error while saving inventory brand", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Unable to save brand.");
        }

        return "redirect:/inventory/brands";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();

            InventoryBrand brand = inventoryBrandService.getById(user, id);

            model.addAttribute("brand", brand);
            model.addAttribute("activePage", "inventory/brands");

            return "inventory/brand-form";

        } catch (IllegalArgumentException e) {
            log.warn("Brand not found. ID: {}, Message: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
            return "redirect:/inventory/brands";

        } catch (Exception e) {
            log.error("Error while loading inventory brand. ID: {}", id, e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Unable to load brand.");
            return "redirect:/inventory/brands";
        }
    }

    @PostMapping("/deactivate/{id}")
    public String deactivateBrand(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();

            inventoryBrandService.deactivate(user, id);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Brand deactivated successfully.");

        } catch (IllegalArgumentException e) {
            log.warn("Unable to deactivate brand. ID: {}, Message: {}",
                    id, e.getMessage());
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());

        } catch (Exception e) {
            log.error("Error while deactivating inventory brand. ID: {}", id, e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Unable to deactivate brand.");
        }

        return "redirect:/inventory/brands";
    }

    @PostMapping("/activate/{id}")
    public String activateBrand(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();

            inventoryBrandService.activate(user, id);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Brand activated successfully.");

        } catch (IllegalArgumentException e) {
            log.warn("Unable to activate brand. ID: {}, Message: {}",
                    id, e.getMessage());
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());

        } catch (Exception e) {
            log.error("Error while activating inventory brand. ID: {}", id, e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Unable to activate brand.");
        }

        return "redirect:/inventory/brands";
    }
}