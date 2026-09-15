package com.example.tailormaster.repository;

import com.example.tailormaster.entity.InventoryBrand;
import com.example.tailormaster.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryBrandRepository extends JpaRepository<InventoryBrand, Long> {

    Optional<InventoryBrand> findByUserAndNameIgnoreCase(User user, String name);

    boolean existsByUserAndNameIgnoreCase(User user, String name);

    List<InventoryBrand> findByUserAndActiveTrueOrderByNameAsc(User user);

    List<InventoryBrand> findByUserOrderByNameAsc(User user);

    Optional<InventoryBrand> findByUserAndId(User user, Long id);
}