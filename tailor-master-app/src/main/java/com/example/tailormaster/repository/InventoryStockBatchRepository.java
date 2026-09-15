package com.example.tailormaster.repository;

import com.example.tailormaster.entity.InventoryStockBatch;
import com.example.tailormaster.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
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
}