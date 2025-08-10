package com.example.tailormaster.repository.order;

import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.User;
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
import java.util.Optional;

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
        AND o.user = :user
    """)
    Page<Order> findBySearchAndDateRangeAndStatus(
            @Param("search") String search,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("user") User user,
            Pageable pageable
    );

    @Query("SELECT o FROM Order o WHERE o.outstandingDueAmount IS NOT NULL AND o.outstandingDueAmount > 0 AND o.user = :user")
    List<Order> findOrdersWithOutstandingDue(@Param("user") User user);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.outstandingDueAmount IS NOT NULL AND o.outstandingDueAmount > 0 AND o.user = :user")
    Long countPendingPayments(@Param("user") User user);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN ('PENDING') AND o.user = :user")
    Long countPendingOrders(@Param("user") User user);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN ('IN_PROGRESS') AND o.user = :user")
    Long countOrdersInProgress(@Param("user") User user);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN ('COMPLETED') AND o.user = :user")
    Long countCompletedOrders(@Param("user") User user);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN ('CANCELLED') AND o.user = :user")
    Long countCancelledOrders(@Param("user") User user);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN ('COMPLETED') AND o.pickupDate IS NULL AND o.pickupStatus IN ('NOT_PICKED_UP') AND o.user = :user")
    Long countOrdersReadyForPickup(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(o.paidAmount), 0) FROM Order o WHERE MONTH(o.pickupDate) = MONTH(CURRENT_DATE) AND YEAR(o.pickupDate) = YEAR(CURRENT_DATE) AND o.user = :user")
    BigDecimal getRevenueThisMonth(@Param("user") User user);

    List<Order> findTop5ByUserOrderByCreatedAtDesc(User user);

    @Query("""
    SELECT o.customer.fullName, o.customer.phoneNumber, COUNT(o), SUM(o.paidAmount)
    FROM Order o
    WHERE o.status = 'COMPLETED' AND o.pickupStatus = 'PICKED_UP' AND o.paidAmount IS NOT NULL
        AND o.user = :currentUser
    GROUP BY o.customer.fullName, o.customer.phoneNumber
    ORDER BY SUM(o.paidAmount) DESC
    """)
    List<Object[]> findTopCustomersWithDetails(User currentUser, Pageable pageable);

    List<Order> findByUserAndDeliveryDateLessThanEqualAndStatusNotIn(
            User user,
            LocalDate deliveryDate,
            List<OrderStatus> excludedStatuses
    );

    Optional<Order> findByIdAndUser(Long id, User currentUser);

    long countByUser(@Param("user") User user);
}
