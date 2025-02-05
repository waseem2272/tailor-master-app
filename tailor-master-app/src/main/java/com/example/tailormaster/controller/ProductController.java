package com.example.tailormaster.controller;

import com.example.tailormaster.dto.ProductDto;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.service.product.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    private ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // List all products
    @GetMapping
    public String listCategories(Model model) {
        List<Product> products = productService.getAllProducts();
        model.addAttribute("products", products);
        return "product/list";
    }

    // get product by id
//    @GetMapping("{id}")
//    public String getProductById(@PathVariable Long id, Model model) {
//        model.addAttribute("product", productService.getProductById(id));
////        model.addAttribute("contextPath", request.getContextPath());
//        return "order/create";
//    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        // Convert to DTO if needed
        ProductDto productDto = new ProductDto(product.getId(), product.getName(), product.getPrice());
        return ResponseEntity.ok(productDto);
    }

    // Show create product form
    @GetMapping("/create")
    public String showCreateProductForm(Model model) {
        model.addAttribute("product", new Product());
        return "product/create";
    }

    // Handle create category form submission
    @PostMapping("/create")
    public String createCategory(@Valid @ModelAttribute("product") Product product,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "product/create";
        }

        try {
            productService.createProduct(product);
            redirectAttributes.addFlashAttribute("successMessage", "Product created successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating product: " + e.getMessage());
        }
        return "redirect:/products";
    }

    // Show update product form
    @GetMapping("/update/{id}")
    public String showUpdateProductForm(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        return "product/update";
    }

    // Handle update product form submission
    @PostMapping("/update/{id}")
    public String updateProduct(
            @PathVariable Long id,
            @Valid @ModelAttribute("product") Product updatedProduct,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            // If validation fails, stay on the update page and display validation errors
            return "product/update";
        }

        try {
            productService.updateProduct(id, updatedProduct);
            redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating Product: " + e.getMessage());
        }
        return "redirect:/products";
    }


    // Delete a Product
    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return "redirect:/products";
    }
}
