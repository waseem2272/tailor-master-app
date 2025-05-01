package com.example.tailormaster.service.customerledger;

import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.ledger.CustomerPaymentLedger;
import com.example.tailormaster.enums.OrderStatus;
import com.example.tailormaster.enums.PaymentType;
import com.example.tailormaster.enums.PickupStatus;
import com.example.tailormaster.repository.customerledger.CustomerPaymentRepository;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@AllArgsConstructor
public class CustomerPaymentLedgerService {
    private static final Logger logger = LogManager.getLogger(CustomerPaymentLedgerService.class);

    private final CustomerPaymentRepository customerPaymentRepository;

    public List<CustomerPaymentLedger> findByCustomerId(Long customerId) {
        return customerPaymentRepository.findByCustomerId(customerId);
    }

    public List<CustomerPaymentLedger> findByCustomerIdOrderByOrderIdAscPaymentDateAsc(Long customerId) {
        return customerPaymentRepository.findByCustomerId(customerId);
    }

    public BigDecimal getRevenueThisMonth() {
        try {
            BigDecimal revenue = customerPaymentRepository.getRevenueThisMonth(PaymentType.DEBIT);
            logger.debug("Revenue this month: {}", revenue);
            return revenue;
        } catch (Exception e) {
            logger.error("Error getting revenue for this month: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting revenue for this month.", e);
        }
    }

    public List<Map<String, Object>> getMonthlyRevenueTrendsForLast6Months() {
        List<Map<String, Object>> trends = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Karachi"));

        try {
            // Calculate date range: from first day of month 5 months ago to end of this month
            YearMonth startMonth = YearMonth.from(today.minusMonths(5));
            LocalDate startDate = startMonth.atDay(1);
            LocalDate endDate = today.withDayOfMonth(today.lengthOfMonth());

            // Call repository method (make sure this method groups by year-month in the query)
            List<Map<String, Object>> rawResults = customerPaymentRepository.getMonthlyRevenueTrends(
                    PaymentType.DEBIT, startDate, endDate
            );

            // Convert raw DB results to month -> revenue map
            Map<String, BigDecimal> monthToRevenue = new HashMap<>();
            for (Map<String, Object> row : rawResults) {
                String month = (String) row.get("month"); // expected format "yyyy-MM"
                BigDecimal total = (BigDecimal) row.get("total");
                monthToRevenue.put(month, total);
            }

            // Format final trends list with fixed 6 months
            for (int i = 5; i >= 0; i--) {
                YearMonth ym = YearMonth.from(today.minusMonths(i));
                String monthKey = ym.format(DateTimeFormatter.ofPattern("yyyy-MM")); // match query format
                String displayMonth = ym.format(DateTimeFormatter.ofPattern("MMM yyyy")); // UI label

                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("month", displayMonth);
                dataPoint.put("revenue", monthToRevenue.getOrDefault(monthKey, BigDecimal.ZERO));
                trends.add(dataPoint);
            }

            logger.debug("Fetched monthly revenue trends for the last 6 months.");
            return trends;

        } catch (Exception e) {
            logger.error("Error fetching monthly revenue trends: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching monthly revenue trends.", e);
        }
    }

}
