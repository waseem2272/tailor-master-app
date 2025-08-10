package com.example.tailormaster.repository.customerledger;

import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.entity.ledger.CustomerPaymentLedger;
import com.example.tailormaster.enums.OrderStatus;
import com.example.tailormaster.enums.PaymentType;
import com.example.tailormaster.enums.PickupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CustomerPaymentRepository extends JpaRepository<CustomerPaymentLedger, Long> {

    List<CustomerPaymentLedger> findByCustomerId(Long customerId);

    @Query("""
    SELECT COALESCE(SUM(o.amount), 0)
    FROM CustomerPaymentLedger o
    WHERE o.paymentType = :paymentType
    AND o.customer.user = :user
      AND MONTH(o.paymentDate) = MONTH(CURRENT_DATE)
      AND YEAR(o.paymentDate) = YEAR(CURRENT_DATE)
""")
    BigDecimal getRevenueThisMonth(@Param("paymentType") PaymentType paymentType, @Param("user") User user);

    @Query("""
    SELECT
        FUNCTION('DATE_FORMAT', o.paymentDate, '%Y-%m') AS month,
        COALESCE(SUM(o.amount), 0) AS total
    FROM CustomerPaymentLedger o
    WHERE o.paymentType = :paymentType
    AND o.customer.user = :user
      AND o.paymentDate BETWEEN :startDate AND :endDate
    GROUP BY FUNCTION('DATE_FORMAT', o.paymentDate, '%Y-%m')
    ORDER BY FUNCTION('DATE_FORMAT', o.paymentDate, '%Y-%m')
""")
    List<Map<String, Object>> getMonthlyRevenueTrends(
            @Param("paymentType") PaymentType paymentType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("user") User user
    );

}
