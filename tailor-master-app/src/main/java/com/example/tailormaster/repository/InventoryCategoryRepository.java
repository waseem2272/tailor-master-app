package com.example.tailormaster.repository;

import com.example.tailormaster.entity.InventoryCategory;
import com.example.tailormaster.enums.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryCategoryRepository extends JpaRepository<InventoryCategory, Long> {

    List<InventoryCategory> findByActiveTrue();

    List<InventoryCategory> findByItemTypeAndActiveTrue(ItemType itemType);

    boolean existsByNameIgnoreCaseAndItemType(String name, ItemType itemType);
}