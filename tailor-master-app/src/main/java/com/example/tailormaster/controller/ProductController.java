package com.example.tailormaster.controller;

import com.example.tailormaster.dto.MeasurementFieldForm;
import com.example.tailormaster.dto.ProductDto;
import com.example.tailormaster.entity.ProductMeasurementField;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.service.ProductMeasurementFieldService;
import com.example.tailormaster.service.product.ProductService;
import com.example.tailormaster.util.AESUtil;
import com.example.tailormaster.util.ThymeleafUtil;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@AllArgsConstructor
@Controller
@RequestMapping("/products")
public class ProductController {

    private static final Logger logger = LogManager.getLogger(ProductController.class);

    private final ProductService productService;
    private final ProductMeasurementFieldService productMeasurementFieldService;

    // List all products
    @GetMapping
    public String listProducts(Model model) {
        logger.info("User accessed the product list page.");
        try {
            List<Product> products = productService.getAllProducts();
            model.addAttribute("products", products);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            logger.info("Retrieved products: {} ", products);
        } catch (Exception e) {
            logger.error("Error retrieving products for the list: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading products.");
        }
        return "product/list";
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        logger.info("Fetching product by ID: {}", id);
        try {
            Product product = productService.getProductById(id);
            if (product == null) {
                logger.warn("Product not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            // Convert to DTO if needed
            ProductDto productDto = new ProductDto(product.getId(), product.getName(), product.getSingleSilai(), product.getDoubleSilai());
            logger.info("Retrieved product with ID {}: {}", id, productDto);
            return ResponseEntity.ok(productDto);
        } catch (Exception e) {
            logger.error("Error fetching product by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Show create product form
    @GetMapping("/create")
    public String showCreateProductForm(Model model) {
        logger.info("Displaying create product form.");
        model.addAttribute("product", new Product());
        return "product/create";
    }

    // Handle create category form submission
    @PostMapping("/create")
    public String createProduct(@Valid @ModelAttribute("product") Product product,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        logger.info("Attempting to create a new product: {}", product);
        if (result.hasErrors()) {
            logger.warn("Validation errors occurred during product creation: {}", result.getAllErrors());
            return "product/create";
        }

        try {
            productService.createProduct(product);
            logger.info("Product created successfully: {}", product);
            redirectAttributes.addFlashAttribute("successMessage", "Product created successfully!");
        } catch (RuntimeException e) {
            logger.error("Error creating product {}: {}", product, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating product: " + e.getMessage());
        }
        return "redirect:/products";
    }

    // Show update product form
    @GetMapping("/update/{id}")
    public String showUpdateProductForm(@PathVariable String id, Model model,
                                        RedirectAttributes redirectAttributes) {
        logger.info("Displaying update product form for product ID: {}", id);
        try {
            // Decrypt and validate product ID
            Long productId = decryptAndValidateProductId(id);
            if (productId == null) {
                logger.warn("Invalid product ID provided for update: {}", id);
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid product ID.");
                return "redirect:/products";
            }
            logger.debug("Decrypted product ID for update: {}", productId);
            Product product = productService.getProductById(productId);
            if (product == null) {
                logger.warn("Product not found with ID {} for update.", productId);
                redirectAttributes.addFlashAttribute("errorMessage", "Product not found.");
                return "redirect:/products";
            }
            model.addAttribute("product", product);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            logger.info("Fetched product for update (ID {}): {}", productId, product);

        } catch (Exception e) {
            logger.error("An error occurred while preparing the update product form for encrypted ID {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while preparing the update product.");
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
        logger.info("Attempting to update product with encrypted ID: {}", encryptedProductId);
        if (bindingResult.hasErrors()) {
            logger.warn("Validation errors occurred during product update for encrypted ID {}: {}", encryptedProductId, bindingResult.getAllErrors());
            return "product/update";
        }

        try {

            // Decrypt and validate product ID
            Long productId = decryptAndValidateProductId(encryptedProductId);
            if (productId == null) {
                logger.warn("Invalid product ID provided for update: {}", encryptedProductId);
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid product ID.");
                return "redirect:/products";
            }
            logger.debug("Decrypted product ID for update: {}", productId);
            productService.updateProduct(productId, updatedProduct);
            logger.info("Product updated successfully with ID: {}", productId);
            redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully!");

        } catch (Exception e) {
            logger.error("An error occurred while updating product with encrypted ID {}: {}", encryptedProductId, e.getMessage(), e);
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
        logger.debug("Attempting to decrypt and validate product ID: {}", encryptedProductId);
        try {
            String decryptedId = AESUtil.decrypt(encryptedProductId);
            if (!decryptedId.matches("\\d+")) {
                logger.warn("Decrypted product ID '{}' is not a valid number.", decryptedId);
                return null;
            }
            Long productId = Long.parseLong(decryptedId);
            logger.debug("Successfully decrypted product ID: {}", productId);
            return productId;
        } catch (Exception e) {
            logger.error("Error decrypting product ID '{}': {}", encryptedProductId, e.getMessage(), e);
            return null;
        }
    }

    @GetMapping("/{id}/measurements")
    public String showMeasurementForm(@PathVariable("id") String encryptedProductId,
                                      @RequestParam(value = "edit", defaultValue = "false") boolean editMode,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        try {
            Long productId = decryptAndValidateProductId(encryptedProductId);
            if (productId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid product ID.");
                return "redirect:/products";
            }

            Product product = productService.getProductById(productId);
            if (product == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Product not found!");
                return "redirect:/products";
            }

            List<ProductMeasurementField> fields = productMeasurementFieldService.getMeasurementFieldsByProductId(productId);

            model.addAttribute("product", product);
            model.addAttribute("fields", fields);
            model.addAttribute("editMode", editMode);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());

            return "product/product-measurements";

        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Something went wrong while loading measurements.");
            return "redirect:/products";
        }
    }

    @PostMapping("/{productId}/measurements")
    public String saveMeasurementFields(
            @PathVariable("productId") String encryptedProductId,
            @ModelAttribute MeasurementFieldForm form,
            RedirectAttributes redirectAttributes) {

        try {

            Long productId = decryptAndValidateProductId(encryptedProductId);
            if (productId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid product ID.");
                return "redirect:/products";
            }

            Product product = productService.getProductById(productId);
            if (product == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Product not found!");
                return "redirect:/products";
            }

            logger.info("Fetching measurement form for productId={}, measurements={}", productId, form.getFields());
            productMeasurementFieldService.saveFields(productId, form.getFields());
            logger.info("Measurements successfully saved for productId={}", productId);
            redirectAttributes.addFlashAttribute("successMessage", "Measurement fields saved!");
            return "redirect:/products/" + new ThymeleafUtil().encryptId(productId) + "/measurements";
        } catch (Exception ex) {
            logger.error("Error while saving measurements for productId={}", encryptedProductId, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Something went wrong while saving measurements.");
            return "redirect:/products";
        }
    }

}
