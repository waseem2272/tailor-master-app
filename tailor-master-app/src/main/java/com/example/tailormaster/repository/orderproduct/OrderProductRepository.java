package com.example.tailormaster.repository.orderproduct;

import com.example.tailormaster.entity.OrderProduct;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.enums.FabricSource;
import com.example.tailormaster.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {
    @Query("""
            SELECT COALESCE(SUM(op.fabricQuantity), 0)
            FROM OrderProduct op
            JOIN op.order o
            WHERE op.inventoryItem.id = :inventoryItemId
              AND op.fabricSource = :fabricSource
              AND o.status = :status
            """)
    BigDecimal getReservedQuantity(
            @Param("inventoryItemId") Long inventoryItemId,
            @Param("fabricSource") FabricSource fabricSource,
            @Param("status") OrderStatus status
    );

    @Query("""
        SELECT COALESCE(SUM(op.fabricQuantity), 0)
        FROM OrderProduct op
        JOIN op.order o
        WHERE op.inventoryItem.id = :inventoryItemId
          AND op.fabricSource = :fabricSource
          AND o.status = :status
          AND o.id <> :orderId
        """)
    BigDecimal getReservedQuantityExcludingOrder(
            @Param("inventoryItemId") Long inventoryItemId,
            @Param("fabricSource") FabricSource fabricSource,
            @Param("status") OrderStatus status,
            @Param("orderId") Long orderId
    );

    Optional<OrderProduct> findByIdAndOrderUser(Long id, User user);
}
