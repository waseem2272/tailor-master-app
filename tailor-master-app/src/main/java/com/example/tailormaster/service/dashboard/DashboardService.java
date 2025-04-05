package com.example.tailormaster.service.dashboard;

import com.example.tailormaster.entity.Order;
import com.example.tailormaster.service.customer.CustomerService;
import com.example.tailormaster.service.order.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Service
public class DashboardService {
    private final CustomerService customerService;
    private final OrderService orderService;

    public long getTotalCustomers() {
        return customerService.getTotalCustomerCount();
    }
    public long getTotalOrders() { return orderService.getTotalOrdersCount(); }
    public long getTotalPendingPayments() { return orderService.getPendingPaymentsCount(); }
    public long getOrdersInProgress() { return orderService.getOrdersInProgressCount(); }
    public long getCompletedOrders() { return orderService.getCompletedOrdersCount(); }
    public BigDecimal getRevenueThisMonth() { return orderService.getRevenueThisMonth(); }
    public long getOrdersReadyForPickup() { return orderService.getOrdersReadyForPickupCount(); }
    public List<Map<String, Object>> getMonthlyRevenueTrendsForLast6Months() { return orderService.getMonthlyRevenueTrendsForLast6Months(); }
    public Map<String, Long> getOrderStatusCounts() { return orderService.getOrderStatusCounts(); }
    public List<Order> getRecentOrders() { return orderService.getTop5ByOrderByCreatedAtDesc(); }

    public List<Map<String, Object>> getTopCustomers() {
        return orderService.getTopCustomers();
    }
}
