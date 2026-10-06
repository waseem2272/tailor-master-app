package com.example.tailormaster.repository;

import com.example.tailormaster.entity.InventoryDesign;
import com.example.tailormaster.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryDesignRepository extends JpaRepository<InventoryDesign, Long> {

    Optional<InventoryDesign> findByUserAndNameIgnoreCase(
            User user,
            String name
    );

    boolean existsByUserAndNameIgnoreCase(
            User user,
            String name
    );

    List<InventoryDesign> findByUserAndActiveTrueOrderByNameAsc(
            User user
    );

    Optional<InventoryDesign> findByUserAndId(
            User user,
            Long id
    );

    List<InventoryDesign> findByUserOrderByCreatedAtDesc(User user);
}