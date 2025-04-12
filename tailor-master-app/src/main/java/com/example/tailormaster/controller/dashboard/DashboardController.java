package com.example.tailormaster.controller.dashboard;

import com.example.tailormaster.entity.Order;
import com.example.tailormaster.enums.OrderStatus;
import com.example.tailormaster.service.dashboard.DashboardService;
import com.example.tailormaster.service.order.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;
    private final OrderService orderService;

    @GetMapping("/total-customers")
    public ResponseEntity<Long> getTotalCustomers() {
        long totalCustomers = dashboardService.getTotalCustomers();
        return ResponseEntity.ok(totalCustomers);
    }

    @GetMapping("/total-orders")
    public ResponseEntity<Long> getTotalOrders() {
        Long totalOrders = dashboardService.getTotalOrders();
        return ResponseEntity.ok(totalOrders);
    }

    @GetMapping("/pending-payments")
    public ResponseEntity<Long> getPendingPayments() {
        Long pendingPayments = dashboardService.getTotalPendingPayments();
        return ResponseEntity.ok(pendingPayments);
    }

    @GetMapping("/orders-in-progress")
    public ResponseEntity<Long> getOrdersInProgress() {
        Long ordersInProgress = dashboardService.getOrdersInProgress();
        return ResponseEntity.ok(ordersInProgress);
    }

    @GetMapping("/completed-orders")
    public ResponseEntity<Long> getCompletedOrders() {
        Long completedOrders = dashboardService.getCompletedOrders();
        return ResponseEntity.ok(completedOrders);
    }

    @GetMapping("/orders-ready-for-pickup")
    public ResponseEntity<Long> getOrdersReadyForPickup() {
        Long ordersReadyForPickup = dashboardService.getOrdersReadyForPickup();
        return ResponseEntity.ok(ordersReadyForPickup);
    }

    @GetMapping("/revenue-this-month")
    public ResponseEntity<BigDecimal> getRevenueThisMonth() {
        BigDecimal revenueThisMonth = dashboardService.getRevenueThisMonth();
        return ResponseEntity.ok(revenueThisMonth);
    }

    @GetMapping("/monthly-revenue-trends")
    @ResponseBody
    public List<Map<String, Object>> getMonthlyRevenueTrends() {
        return dashboardService.getMonthlyRevenueTrendsForLast6Months();
    }

    @GetMapping("/order-status-counts")
    @ResponseBody
    public Map<String, Long> getOrderStatusCounts() {
        return dashboardService.getOrderStatusCounts(); // Implement this in your OrderService
    }

    @GetMapping("/recent-orders")
    public List<Map<String, Object>> getRecentOrders() {
        List<Order> recentOrders = dashboardService.getRecentOrders();

        return recentOrders.stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("orderId", order.getOrderId());
            map.put("customer", order.getCustomer().getFullName());
            map.put("orderDate", order.getOrderDate());
            map.put("status", order.getStatus().name());
            map.put("totalAmount", order.getTotalProductAmount());
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/customers/top")
    public List<Map<String, Object>> getTopCustomers() {
        return dashboardService.getTopCustomers();
    }

    @GetMapping("/notifications/upcoming-deliveries")
    public List<Map<String, Object>> getUpcomingDeliveries() {
        return dashboardService.getUpcomingDeliveries();
    }


}
