package com.example.tailormaster.repository;

import com.example.tailormaster.entity.OrderInventoryReservation;
import com.example.tailormaster.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderInventoryReservationRepository
        extends JpaRepository<OrderInventoryReservation, Long> {

    List<OrderInventoryReservation> findByUserAndOrderIdAndReleasedFalse(
            User user,
            Long orderId
    );

    boolean existsByUserAndOrderProductIdAndReleasedFalse(
            User user,
            Long orderProductId
    );

    List<OrderInventoryReservation> findByUserAndOrderProductIdAndReleasedFalse(
            User user,
            Long orderProductId
    );

    List<OrderInventoryReservation> findByUserAndInventoryItemIdAndReleasedFalse(
            User user,
            Long inventoryItemId
    );

    @Query("""
        SELECT COALESCE(SUM(r.quantity), 0)
        FROM OrderInventoryReservation r
        WHERE r.user = :user
          AND r.inventoryItem.id = :inventoryItemId
          AND r.released = false
        """)
    BigDecimal getReservedQuantity(
            @Param("user") User user,
            @Param("inventoryItemId") Long inventoryItemId
    );
}