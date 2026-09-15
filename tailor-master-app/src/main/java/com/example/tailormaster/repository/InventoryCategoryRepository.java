package com.example.tailormaster.repository;

import com.example.tailormaster.entity.InventoryCategory;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.enums.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryCategoryRepository extends JpaRepository<InventoryCategory, Long> {

    List<InventoryCategory> findByUserAndActiveTrueOrderByNameAsc(User user);

    List<InventoryCategory> findByUserAndItemTypeAndActiveTrueOrderByNameAsc(
            User user, ItemType itemType);

    List<InventoryCategory> findByUserOrderByNameAsc(User user);

    boolean existsByUserAndNameIgnoreCaseAndItemType(
            User user, String name, ItemType itemType);

    Optional<InventoryCategory> findByUserAndId(User user, Long id);
}