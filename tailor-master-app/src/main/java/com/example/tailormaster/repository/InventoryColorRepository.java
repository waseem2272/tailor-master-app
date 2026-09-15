package com.example.tailormaster.repository;

import com.example.tailormaster.entity.InventoryColor;
import com.example.tailormaster.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryColorRepository extends JpaRepository<InventoryColor, Long> {

    Optional<InventoryColor> findByUserAndNameIgnoreCase(
            User user,
            String name
    );

    boolean existsByUserAndNameIgnoreCase(
            User user,
            String name
    );

    List<InventoryColor> findByUserAndActiveTrueOrderByNameAsc(
            User user
    );

    Optional<InventoryColor> findByUserAndId(
            User user,
            Long id
    );

    List<InventoryColor> findByUserOrderByNameAsc(User user);
}