package com.example.tailormaster.repository;

import com.example.tailormaster.entity.StockMovement;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.enums.StockMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByUserAndInventoryItemIdOrderByCreatedAtDesc(
            User user, Long inventoryItemId);

    List<StockMovement> findByUserAndMovementTypeOrderByCreatedAtDesc(
            User user, StockMovementType movementType);

    List<StockMovement> findByUserAndInventoryItemIdAndMovementTypeOrderByCreatedAtDesc(
            User user,
            Long inventoryItemId,
            StockMovementType movementType);

    List<StockMovement> findByUserOrderByCreatedAtDesc(User user);
}