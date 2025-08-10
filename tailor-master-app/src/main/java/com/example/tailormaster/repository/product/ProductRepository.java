package com.example.tailormaster.repository.product;

import com.example.tailormaster.entity.User;
import com.example.tailormaster.entity.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByName(String name);

    // active products
    @Query("SELECT p FROM Product p WHERE p.enabled = true")
    List<Product> findAllActiveProducts();

    List<Product> findAllByUser(User user);

    List<Product> findAllActiveProductsByUser(User currentUser);

    Optional<Product> findByIdAndUser(Long id, User currentUser);
}
