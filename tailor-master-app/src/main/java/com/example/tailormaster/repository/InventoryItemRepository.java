package com.example.tailormaster.repository;

import com.example.tailormaster.entity.InventoryItem;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.enums.ItemType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByUserAndActiveTrue(User user);

    List<InventoryItem> findByUserAndItemTypeAndActiveTrue(User user, ItemType itemType);

    List<InventoryItem> findByUserAndBrandIdAndActiveTrue(User user, Long brandId);

    List<InventoryItem> findByUserAndCategoryIdAndActiveTrue(User user, Long categoryId);

    List<InventoryItem> findByUserAndNameContainingIgnoreCaseAndActiveTrue(User user, String name);

    List<InventoryItem> findByUserAndActiveTrueOrderByNameAsc(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.id = :id AND i.user = :user")
    Optional<InventoryItem> findByIdForUpdate(@Param("id") Long id, @Param("user") User user);

    Optional<InventoryItem> findByUserAndId(User user, Long id);
}