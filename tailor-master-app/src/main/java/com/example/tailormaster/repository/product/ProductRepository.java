package com.example.tailormaster.repository.product;

import com.example.tailormaster.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByName(String name);

    // active products
    @Query("SELECT p FROM Product p WHERE p.enabled = true")
    List<Product> findAllActiveProducts();}
