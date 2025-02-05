package com.example.tailormaster.service.product;

import com.example.tailormaster.entity.ProductCategory;
import com.example.tailormaster.repository.product.ProductCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductCategoryService {

    private ProductCategoryRepository categoryRepository;

    public ProductCategoryService(ProductCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<ProductCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    public ProductCategory getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }

    public void createCategory(ProductCategory category) {
        if (categoryRepository.existsByName(category.getName())) {
            throw new RuntimeException("Category with the same name already exists.");
        }
        categoryRepository.save(category);
    }

    public void updateCategory(Long id, ProductCategory updatedCategory) {
        ProductCategory category = getCategoryById(id);
        category.setName(updatedCategory.getName());
        category.setDescription(updatedCategory.getDescription());
        categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }
}
