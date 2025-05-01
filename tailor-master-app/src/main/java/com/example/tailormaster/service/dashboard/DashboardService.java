package com.example.tailormaster.service.dashboard;

import com.example.tailormaster.entity.Order;
import com.example.tailormaster.service.customer.CustomerService;
import com.example.tailormaster.service.customerledger.CustomerPaymentLedgerService;
import com.example.tailormaster.service.order.OrderService;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Service
public class DashboardService {

    private static final Logger logger = LogManager.getLogger(DashboardService.class);

    private final CustomerService customerService;
    private final OrderService orderService;
    private final CustomerPaymentLedgerService customerPaymentLedgerService;

    public long getTotalCustomers() {
        logger.debug("Fetching total customer count");
        long count = customerService.getTotalCustomerCount();
        logger.info("Total customers: {}", count);
        return count;
    }

    public long getTotalOrders() {
        logger.debug("Fetching total order count");
        long count = orderService.getTotalOrdersCount();
        logger.info("Total orders: {}", count);
        return count;
    }

    public long getTotalPendingPayments() {
        logger.debug("Fetching pending payments count");
        long count = orderService.getPendingPaymentsCount();
        logger.info("Total pending payments: {}", count);
        return count;
    }

    public long getOrdersInProgress() {
        logger.debug("Fetching in-progress orders count");
        long count = orderService.getOrdersInProgressCount();
        logger.info("Orders in progress: {}", count);
        return count;
    }

    public long getCompletedOrders() {
        logger.debug("Fetching completed orders count");
        long count = orderService.getCompletedOrdersCount();
        logger.info("Completed orders: {}", count);
        return count;
    }

    public BigDecimal getRevenueThisMonth() {
        logger.debug("Fetching revenue for current month");
        BigDecimal revenue = customerPaymentLedgerService.getRevenueThisMonth();
        logger.info("Revenue this month: {}", revenue);
        return revenue;
    }

    public long getOrdersReadyForPickup() {
        logger.debug("Fetching orders ready for pickup count");
        long count = orderService.getOrdersReadyForPickupCount();
        logger.info("Orders ready for pickup: {}", count);
        return count;
    }

    public List<Map<String, Object>> getMonthlyRevenueTrendsForLast6Months() {
        logger.debug("Fetching monthly revenue trends for last 6 months");
        List<Map<String, Object>> trends = customerPaymentLedgerService.getMonthlyRevenueTrendsForLast6Months();
        logger.info("Monthly revenue trends fetched successfully");
        return trends;
    }

    public Map<String, Long> getOrderStatusCounts() {
        logger.debug("Fetching order status counts");
        Map<String, Long> counts = orderService.getOrderStatusCounts();
        logger.info("Order status counts: {}", counts);
        return counts;
    }

    public List<Order> getRecentOrders() {
        logger.debug("Fetching top 5 recent orders");
        List<Order> recentOrders = orderService.getTop5ByOrderByCreatedAtDesc();
        logger.info("Recent orders fetched: {} orders", recentOrders.size());
        return recentOrders;
    }

    public List<Map<String, Object>> getTopCustomers() {
        logger.debug("Fetching top customers");
        List<Map<String, Object>> topCustomers = orderService.getTopCustomers();
        logger.info("Top customers fetched: {}", topCustomers.size());
        return topCustomers;
    }

    public List<Map<String, Object>> getUpcomingDeliveries() {
        logger.debug("Fetching upcoming deliveries");
        List<Map<String, Object>> deliveries = orderService.getUpcomingDeliveries();
        logger.info("Upcoming deliveries fetched: {}", deliveries.size());
        return deliveries;
    }
}
