package com.example.tailormaster.controller;

import com.example.tailormaster.entity.ProductCategory;
import com.example.tailormaster.service.product.ProductCategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/categories")
public class ProductCategoryController {

    private ProductCategoryService categoryService;

    public ProductCategoryController(ProductCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // List all categories
    @GetMapping
    public String listCategories(Model model) {
        List<ProductCategory> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        return "categories/list";
    }

    // Show create category form
    @GetMapping("/create")
    public String showCreateCategoryForm(Model model) {
        model.addAttribute("category", new ProductCategory());
        return "categories/create";
    }

    // Handle create category form submission
    @PostMapping("/create")
    public String createCategory(@ModelAttribute("category") ProductCategory category, RedirectAttributes redirectAttributes) {
        try {
            categoryService.createCategory(category);
            redirectAttributes.addFlashAttribute("successMessage", "Category created successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating category: " + e.getMessage());
        }
        return "redirect:/categories";
    }

    // Show update category form
    @GetMapping("/update/{id}")
    public String showUpdateCategoryForm(@PathVariable Long id, Model model) {
        ProductCategory category = categoryService.getCategoryById(id);
        model.addAttribute("category", category);
        return "categories/update";
    }

    // Handle update category form submission
    @PostMapping("/update/{id}")
    public String updateCategory(@PathVariable Long id, @ModelAttribute("category") ProductCategory updatedCategory, RedirectAttributes redirectAttributes) {
        try {
            categoryService.updateCategory(id, updatedCategory);
            redirectAttributes.addFlashAttribute("successMessage", "Category updated successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating category: " + e.getMessage());
        }
        return "redirect:/categories";
    }

    // Delete a category
    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return "redirect:/categories";
    }
}
