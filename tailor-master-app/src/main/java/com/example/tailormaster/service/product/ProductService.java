package com.example.tailormaster.service.product;

import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.repository.product.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> getAllActiveProducts() {
        return productRepository.findAllActiveProducts();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    public void createProduct(Product product) {
        if (productRepository.existsByName(product.getName())) {
            throw new RuntimeException("Product with the same name already exists.");
        }
        productRepository.save(product);
    }

    public void updateProduct(Long id, Product updatedProduct) {
        Product product = getProductById(id);
        product.setName(updatedProduct.getName());
        product.setPrice(updatedProduct.getPrice());
        product.setDescription(updatedProduct.getDescription());
        product.setEnabled(updatedProduct.isEnabled());
        productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }
}
