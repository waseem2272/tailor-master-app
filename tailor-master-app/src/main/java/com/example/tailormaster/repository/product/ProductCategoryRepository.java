package com.example.tailormaster.repository.product;

import com.example.tailormaster.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    boolean existsByName(String name);
}
