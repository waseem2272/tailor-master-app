package com.example.tailormaster.repository.order;

import com.example.tailormaster.entity.Order;
import com.example.tailormaster.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Fetch orders by customer ID
    List<Order> findByCustomerId(Long customerId);

    // Fetch orders by status
    List<Order> findByStatus(OrderStatus status);

    @Query("SELECT o.orderId FROM Order o WHERE o.user.id = :userId ORDER BY o.id DESC")
    List<String> findLastOrderIdForUser(@Param("userId") Long userId, Pageable pageable);

//    @Query("SELECT o FROM Order o WHERE LOWER(o.orderId) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.customer.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR o.customer.phoneNumber LIKE %:search%")
//    Page<Order> findByOrderIdOrCustomerFullNameOrPhoneNumber(@Param("search") String search, Pageable pageable);

    @Query("""
    SELECT o FROM Order o
    WHERE (:search IS NULL OR LOWER(o.orderId) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(o.customer.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
        OR o.customer.phoneNumber LIKE %:search%)
    AND (:startDate IS NULL OR o.orderDate >= :startDate)
    AND (:endDate IS NULL OR o.orderDate <= :endDate)
    """)
    Page<Order> findBySearchAndDateRange(
            @Param("search") String search,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("SELECT o FROM Order o WHERE o.outstandingDueAmount IS NOT NULL AND o.outstandingDueAmount > 0")
    List<Order> findOrdersWithOutstandingDue();
}
