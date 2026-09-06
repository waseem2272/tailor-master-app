package com.example.tailormaster.controller.dashboard;

import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.service.dashboard.DashboardService;
import com.example.tailormaster.service.order.OrderService;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private static final Logger logger = LogManager.getLogger(DashboardController.class);

    private final DashboardService dashboardService;
    private final OrderService orderService;

    @GetMapping("/total-customers")
    public ResponseEntity<Long> getTotalCustomers() {
        logger.info("Requested total customers.");
        try {
            long totalCustomers = dashboardService.getTotalCustomers();
            logger.info("Total customers: {}", totalCustomers);
            return ResponseEntity.ok(totalCustomers);
        } catch (Exception e) {
            logger.error("Error fetching total customers: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/total-orders")
    public ResponseEntity<Long> getTotalOrders() {
        logger.info("Requested total orders.");
        try {
            Long totalOrders = dashboardService.getTotalOrders();
            logger.info("Total orders: {}", totalOrders);
            return ResponseEntity.ok(totalOrders);
        } catch (Exception e) {
            logger.error("Error fetching total orders: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/pending-payments")
    public ResponseEntity<Long> getPendingPayments() {
        logger.info("Requested pending payments.");
        try {
            Long pendingPayments = dashboardService.getTotalPendingPayments();
            logger.info("Pending payments: {}", pendingPayments);
            return ResponseEntity.ok(pendingPayments);
        } catch (Exception e) {
            logger.error("Error fetching pending payments: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/pending-payments-summary")
    @ResponseBody
    public Map<String, Object> getPendingPaymentsSummary() {

        Map<String, Object> response = new HashMap<>();

        try {

            Object[] result = orderService.getPendingPaymentsSummary();

            if (result != null) {

                // Agar result ke andar actual row Object[] hai
                if (result.length == 1 && result[0] instanceof Object[]) {

                    Object[] row = (Object[]) result[0];

                    response.put("count", row.length > 0 ? row[0] : 0L);
                    response.put("amount", row.length > 1 ? row[1] : BigDecimal.ZERO);

                } else {

                    // Normal case: [count, amount]
                    response.put("count", result.length > 0 ? result[0] : 0L);
                    response.put("amount", result.length > 1 ? result[1] : BigDecimal.ZERO);
                }

            } else {
                response.put("count", 0L);
                response.put("amount", BigDecimal.ZERO);
            }

        } catch (Exception e) {

            logger.error("Error while fetching pending payments summary", e);

            response.put("count", 0L);
            response.put("amount", BigDecimal.ZERO);
        }

        return response;
    }

    @GetMapping("/orders-in-progress")
    public ResponseEntity<Long> getOrdersInProgress() {
        logger.info("Requested orders in-progress.");
        try {
            Long ordersInProgress = dashboardService.getOrdersInProgress();
            logger.info("Orders in progress: {}", ordersInProgress);
            return ResponseEntity.ok(ordersInProgress);
        } catch (Exception e) {
            logger.error("Error fetching orders in progress: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/completed-orders")
    public ResponseEntity<Long> getCompletedOrders() {
        logger.info("Requested completed orders.");
        try {
            Long completedOrders = dashboardService.getCompletedOrders();
            logger.info("Completed orders: {}", completedOrders);
            return ResponseEntity.ok(completedOrders);
        } catch (Exception e) {
            logger.error("Error fetching completed orders: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/revenue-this-month")
    public ResponseEntity<BigDecimal> getRevenueThisMonth() {
        logger.info("Requested revenue-this-month.");
        try {
            BigDecimal revenueThisMonth = dashboardService.getRevenueThisMonth();
            logger.info("Revenue this month: {}", revenueThisMonth);
            return ResponseEntity.ok(revenueThisMonth);
        } catch (Exception e) {
            logger.error("Error fetching revenue for this month: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/monthly-revenue-trends")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getMonthlyRevenueTrends() {
        logger.info("Requested monthly-revenue-trends.");
        try {
            List<Map<String, Object>> revenueTrends = dashboardService.getMonthlyRevenueTrendsForLast6Months();
            return new ResponseEntity<>(revenueTrends, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching monthly revenue trends: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/order-status-counts")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getOrderStatusCounts() {
        logger.info("Requested order-status-counts.");
        try {
            Map<String, Long> orderStatusCounts = dashboardService.getOrderStatusCounts();
            logger.info("Order status counts: {}", orderStatusCounts);
            return ResponseEntity.ok(orderStatusCounts);
        } catch (Exception e) {
            logger.error("Error fetching order status counts: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/recent-orders")
    public ResponseEntity<List<Map<String, Object>>> getRecentOrders() {
        logger.info("Requested recent-orders.");
        try {
            List<Order> recentOrders = dashboardService.getRecentOrders();
            List<Map<String, Object>> response = recentOrders.stream().map(order -> {
                Map<String, Object> map = new HashMap<>();
                map.put("orderId", order.getOrderId());
                map.put("customer", order.getCustomer().getFullName());
                map.put("orderDate", order.getOrderDate());
                map.put("status", order.getStatus().name());
                map.put("totalAmount", order.getTotalProductAmount());
                return map;
            }).collect(Collectors.toList());
            logger.info("Recent orders fetched: {}", response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching recent orders: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/customers/top")
    public ResponseEntity<List<Map<String, Object>>> getTopCustomers() {
        logger.info("Requested top customers.");
        try {
            List<Map<String, Object>> topCustomers = dashboardService.getTopCustomers();
            logger.info("Top customers fetched: {}", topCustomers.size());
            return ResponseEntity.ok(topCustomers);
        } catch (Exception e) {
            logger.error("Error fetching top customers: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/notifications/upcoming-deliveries")
    public ResponseEntity<List<Map<String, Object>>> getUpcomingDeliveries() {
        logger.info("Requested upcoming deliveries.");
        try {
            List<Map<String, Object>> upcomingDeliveries = dashboardService.getUpcomingDeliveries();
            logger.info("Upcoming deliveries fetched: {}", upcomingDeliveries.size());
            return ResponseEntity.ok(upcomingDeliveries);
        } catch (Exception e) {
            logger.error("Error fetching upcoming deliveries: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
