package com.example.tailormaster.service.product;

import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.repository.product.ProductRepository;
import com.example.tailormaster.util.AuthenticatedUserService;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class ProductService {

    private static final Logger logger = LogManager.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public List<Product> getAllProducts() {
        logger.info("Fetching all products.");
        try {
            List<Product> products = productRepository.findAllByUser(authenticatedUserService.getCurrentUser());
            logger.debug("Retrieved {} products.", products.size());
            return products;
        } catch (Exception e) {
            logger.error("Error fetching all products: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching all products.", e);
        }
    }

    public List<Product> getAllActiveProducts() {
        logger.info("Fetching all active products.");
        try {
            List<Product> activeProducts = productRepository.findAllActiveProductsByUser(authenticatedUserService.getCurrentUser());
            logger.info("Retrieved {} active products.", activeProducts.size());
            return activeProducts;
        } catch (Exception e) {
            logger.error("Error fetching all active products: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching all active products.", e);
        }
    }

    public Product getProductById(Long id) {
        logger.info("Fetching product by ID: {}", id);
        try {
            Product product = productRepository.findByIdAndUser(id, authenticatedUserService.getCurrentUser())
                    .orElseThrow(() -> {
                        logger.warn("Product not found with ID: {}", id);
                        return new RuntimeException("Product not found with id: " + id);
                    });
            logger.debug("Retrieved product with ID {}: {}", id, product);
            return product;
        } catch (RuntimeException e) {
            // Log the exception that was intentionally thrown
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching product by ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error fetching product by ID.", e);
        }
    }

    public void createProduct(Product product) {
        logger.info("Creating a new product: {}", product);
        try {
            if (productRepository.existsByName(product.getName())) {
                logger.warn("Product with the name '{}' already exists.", product.getName());
                throw new RuntimeException("Product with the same name already exists.");
            }
            product.setUser(authenticatedUserService.getCurrentUser());
            Product savedProduct = productRepository.save(product);
            logger.info("Product created successfully with ID: {}", savedProduct.getId());
        } catch (RuntimeException e) {
            // Log the exception that was intentionally thrown
            throw e;
        } catch (Exception e) {
            logger.error("Error creating product {}: {}", product, e.getMessage(), e);
            throw new RuntimeException("Error creating product.", e);
        }
    }

    public void updateProduct(Long id, Product updatedProduct) {
        logger.info("Updating product with ID {}: {}", id, updatedProduct);
        try {
            Product product = getProductById(id);
            product.setName(updatedProduct.getName());
            product.setPrice(updatedProduct.getPrice());
            product.setDescription(updatedProduct.getDescription());
            product.setEnabled(updatedProduct.isEnabled());
            product.setUser(authenticatedUserService.getCurrentUser());
            Product savedProduct = productRepository.save(product);
            logger.debug("Product with ID {} updated successfully: {}", id, savedProduct);
        } catch (RuntimeException e) {
            // Log the exception that was intentionally thrown by getProductById
            throw e;
        } catch (Exception e) {
            logger.error("Error updating product with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error updating product.", e);
        }
    }

    public void deleteProduct(Long id) {
        logger.info("Deleting product with ID: {}", id);
        try {
            if (!productRepository.existsById(id)) {
                logger.warn("Product not found with ID {} for deletion.", id);
                throw new RuntimeException("Product not found with id: " + id);
            }
            productRepository.deleteById(id);
            logger.info("Product with ID {} deleted successfully.", id);
        } catch (RuntimeException e) {
            // Log the exception that was intentionally thrown
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting product with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error deleting product.", e);
        }
    }
}
