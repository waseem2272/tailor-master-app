package com.example.tailormaster.repository;

import com.example.tailormaster.entity.InventoryStockBatch;
import com.example.tailormaster.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface InventoryStockBatchRepository extends JpaRepository<InventoryStockBatch, Long> {

    List<InventoryStockBatch> findByInventoryItem_UserAndInventoryItem_IdOrderByReceivedDateAscIdAsc(
            User user,
            Long inventoryItemId
    );

    List<InventoryStockBatch> findByUserAndInventoryItemIdAndRemainingQuantityGreaterThanOrderByReceivedDateAscIdAsc(
            User user,
            Long inventoryItemId,
            BigDecimal quantity
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM InventoryStockBatch b
            WHERE b.user = :user
              AND b.inventoryItem.id = :inventoryItemId
              AND b.remainingQuantity > 0
            ORDER BY b.receivedDate ASC, b.id ASC
            """)
    List<InventoryStockBatch> findAvailableBatchesForUpdate(
            @Param("user") User user,
            @Param("inventoryItemId") Long inventoryItemId
    );
}