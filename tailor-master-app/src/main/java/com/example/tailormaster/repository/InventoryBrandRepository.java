package com.example.tailormaster.repository;

import com.example.tailormaster.entity.InventoryBrand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryBrandRepository extends JpaRepository<InventoryBrand, Long> {

    Optional<InventoryBrand> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}