package com.example.tailormaster.repository;

import com.example.tailormaster.entity.StockMovement;
import com.example.tailormaster.enums.StockMovementType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByInventoryItemIdOrderByCreatedAtDesc(Long inventoryItemId);

    List<StockMovement> findByMovementTypeOrderByCreatedAtDesc(StockMovementType movementType);

    List<StockMovement> findByInventoryItemIdAndMovementTypeOrderByCreatedAtDesc(
            Long inventoryItemId,
            StockMovementType movementType
    );

    List<StockMovement> findAllByOrderByCreatedAtDesc();
}