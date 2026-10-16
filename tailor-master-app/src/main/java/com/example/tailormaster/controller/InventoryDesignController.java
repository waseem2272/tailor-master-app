package com.example.tailormaster.controller;

import com.example.tailormaster.entity.InventoryDesign;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.service.InventoryDesignService;
import com.example.tailormaster.util.AuthenticatedUserService;
import com.example.tailormaster.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventory/designs")
@RequiredArgsConstructor
@Slf4j
public class InventoryDesignController {

    private final InventoryDesignService inventoryDesignService;
    private final AuthenticatedUserService authenticatedUserService;
    private final MessageUtil messageUtil;

    @GetMapping
    public String listDesigns(Model model) {
        User user = getCurrentUser();
        model.addAttribute("designs", inventoryDesignService.getAllDesigns(user));
        model.addAttribute("activePage", "inventory/designs");
        return "inventory/designs";
    }

    @GetMapping("/add")
    public String addDesignForm(Model model) {
        model.addAttribute("design", new InventoryDesign());
        model.addAttribute("activePage", "inventory/designs");
        return "inventory/design-form";
    }

    @PostMapping("/save")
    public String saveDesign(
            @ModelAttribute("design") InventoryDesign design,
            RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();
            inventoryDesignService.save(user, design);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    messageUtil.get("inventory.design.success.save")
            );
        } catch (IllegalArgumentException e) {
            log.warn("Unable to save inventory design: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error while saving inventory design", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    messageUtil.get("inventory.design.error.save")
            );
        }
        return "redirect:/inventory/designs";
    }

    @GetMapping("/edit/{id}")
    public String editDesign(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();
            InventoryDesign design = inventoryDesignService.findById(user, id)
                    .orElseThrow(() -> new IllegalArgumentException(
                            messageUtil.get("inventory.design.error.notFound")
                    ));
            model.addAttribute("design", design);
            model.addAttribute("activePage", "inventory/designs");
            return "inventory/design-form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/inventory/designs";
        } catch (Exception e) {
            log.error("Error while loading inventory design. ID: {}", id, e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    messageUtil.get("inventory.design.error.load")
            );
            return "redirect:/inventory/designs";
        }
    }

    @PostMapping("/deactivate/{id}")
    public String deactivateDesign(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();
            inventoryDesignService.deactivate(user, id);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    messageUtil.get("inventory.design.success.deactivate")
            );
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error while deactivating inventory design. ID: {}", id, e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    messageUtil.get("inventory.design.error.deactivate")
            );
        }
        return "redirect:/inventory/designs";
    }

    @PostMapping("/activate/{id}")
    public String activateDesign(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            User user = getCurrentUser();
            inventoryDesignService.activate(user, id);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    messageUtil.get("inventory.design.success.activate")
            );
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error while activating inventory design. ID: {}", id, e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    messageUtil.get("inventory.design.error.activate")
            );
        }
        return "redirect:/inventory/designs";
    }

    private User getCurrentUser() {
        return authenticatedUserService.getCurrentUser();
    }
}