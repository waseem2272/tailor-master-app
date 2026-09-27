package com.example.tailormaster.repository;

import com.example.tailormaster.entity.StockMovement;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.enums.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByUserOrderByCreatedAtDesc(User user);

    Page<StockMovement> findByUserAndInventoryItemIdOrderByCreatedAtDesc(
            User user,
            Long inventoryItemId,
            Pageable pageable);

    Page<StockMovement> findByUserAndMovementTypeOrderByCreatedAtDesc(
            User user,
            StockMovementType movementType,
            Pageable pageable);

    Page<StockMovement> findByUserAndInventoryItemIdAndMovementTypeOrderByCreatedAtDesc(
            User user,
            Long inventoryItemId,
            StockMovementType movementType,
            Pageable pageable);

    Page<StockMovement> findByUserOrderByCreatedAtDesc(
            User user,
            Pageable pageable);
}