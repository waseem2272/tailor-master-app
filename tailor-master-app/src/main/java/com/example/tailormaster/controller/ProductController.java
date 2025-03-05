package com.example.tailormaster.controller;

import com.example.tailormaster.dto.ProductDto;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.service.product.ProductService;
import com.example.tailormaster.util.AESUtil;
import com.example.tailormaster.util.ThymeleafUtil;
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

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // List all products
    @GetMapping
    public String listProducts(Model model) {
        List<Product> products = productService.getAllProducts();
        model.addAttribute("products", products);
        model.addAttribute("thymeleafUtil", new ThymeleafUtil());
        return "product/list";
    }

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
    public String createProduct(@Valid @ModelAttribute("product") Product product,
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
    public String showUpdateProductForm(@PathVariable String id, Model model,
                                        RedirectAttributes redirectAttributes) {

        try {
            // Decrypt and validate product ID
            Long productId = decryptAndValidateProductId(id);
            if (productId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid product ID.");
                return "redirect:/products";
            }

            Product product = productService.getProductById(productId);
            model.addAttribute("product", product);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to decrypt product ID.");
            return "redirect:/products";
        }

        return "product/update";
    }

    // Handle update product form submission
    @PostMapping("/update")
    public String updateProduct(
            @Valid @ModelAttribute("product") Product updatedProduct,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            @RequestParam("encryptedProductId") String encryptedProductId) {

        if (bindingResult.hasErrors()) {
            // If validation fails, stay on the update page and display validation errors
            return "product/update";
        }

        try {

            // Decrypt and validate product ID
            Long productId = decryptAndValidateProductId(encryptedProductId);
            if (productId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid product ID.");
                return "redirect:/products";
            }

            productService.updateProduct(productId, updatedProduct);
            redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully!");

        } catch (Exception e) {
            e.printStackTrace();
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

    private Long decryptAndValidateProductId(String encryptedProductId) {
        try {
            String decryptedId = AESUtil.decrypt(encryptedProductId);
            if (!decryptedId.matches("\\d+")) {
                return null;
            }
            return Long.parseLong(decryptedId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
