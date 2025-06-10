package com.example.tailormaster.repository.order;

import com.example.tailormaster.entity.Order;
import com.example.tailormaster.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o.orderId FROM Order o WHERE o.user.id = :userId ORDER BY o.id DESC")
    List<String> findLastOrderIdForUser(@Param("userId") Long userId, Pageable pageable);

    @Query("""
    SELECT o FROM Order o
    WHERE (:search IS NULL OR LOWER(o.orderId) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(o.customer.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
        OR o.customer.phoneNumber LIKE %:search%)
    AND (:startDate IS NULL OR o.orderDate >= :startDate)
    AND (:endDate IS NULL OR o.orderDate <= :endDate)
    AND (:orderStatus IS NULL OR o.status = :orderStatus)
    """)
    Page<Order> findBySearchAndDateRangeAndStatus(
            @Param("search") String search,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("orderStatus") OrderStatus orderStatus,
            Pageable pageable
    );

    @Query("SELECT o FROM Order o WHERE o.outstandingDueAmount IS NOT NULL AND o.outstandingDueAmount > 0")
    List<Order> findOrdersWithOutstandingDue();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.outstandingDueAmount IS NOT NULL AND o.outstandingDueAmount > 0")
    Long countPendingPayments();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN ('PENDING')")
    Long countPendingOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN ('IN_PROGRESS')")
    Long countOrdersInProgress();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN ('COMPLETED')")
    Long countCompletedOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN ('CANCELLED')")
    Long countCancelledOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN ('COMPLETED') AND o.pickupDate IS NULL AND o.pickupStatus IN ('NOT_PICKED_UP')")
    Long countOrdersReadyForPickup();

    @Query("SELECT COALESCE(SUM(o.paidAmount), 0) FROM Order o WHERE MONTH(o.pickupDate) = MONTH(CURRENT_DATE) AND YEAR(o.pickupDate) = YEAR(CURRENT_DATE)")
    BigDecimal getRevenueThisMonth();

    List<Order> findTop5ByOrderByCreatedAtDesc();

    @Query("""
    SELECT o.customer.fullName, o.customer.phoneNumber, COUNT(o), SUM(o.paidAmount)
    FROM Order o
    WHERE o.status = 'COMPLETED' AND o.pickupStatus = 'PICKED_UP' AND o.paidAmount IS NOT NULL
    GROUP BY o.customer.fullName, o.customer.phoneNumber
    ORDER BY SUM(o.paidAmount) DESC
    """)
    List<Object[]> findTopCustomersWithDetails(Pageable pageable);

    List<Order> findByDeliveryDateLessThanEqualAndStatusNotIn(
            LocalDate from, List<OrderStatus> excludedStatuses);

}
