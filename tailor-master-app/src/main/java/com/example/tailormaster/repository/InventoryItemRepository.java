package com.example.tailormaster.repository;

import com.example.tailormaster.entity.InventoryItem;
import com.example.tailormaster.enums.ItemType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByActiveTrue();

    List<InventoryItem> findByItemTypeAndActiveTrue(ItemType itemType);

    List<InventoryItem> findByBrandIdAndActiveTrue(Long brandId);

    List<InventoryItem> findByCategoryIdAndActiveTrue(Long categoryId);

    List<InventoryItem> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    List<InventoryItem> findByActiveTrueOrderByNameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.id = :id")
    Optional<InventoryItem> findByIdForUpdate(@Param("id") Long id);
}