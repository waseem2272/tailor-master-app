package com.example.tailormaster.service.order;

import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.OrderStatus;
import com.example.tailormaster.repository.order.OrderRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    // Save an order
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    // Get all orders
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // Get order by ID
    public Order findById(Long id) {
        Optional<Order> order = orderRepository.findById(id);
        return order.orElse(null);
    }

    // Get orders by customer ID
    public List<Order> findByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    // Get orders by status (Enum-based)
    public List<Order> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    // Update order status
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = findById(orderId);
        if (order != null) {
            order.setStatus(newStatus);
            return orderRepository.save(order);
        }
        return null;
    }

    // Delete an order
    public void deleteById(Long id) {
        orderRepository.deleteById(id);
    }

    public int getNextOrderNumberForUser(Long userId) {
        Pageable pageable = PageRequest.of(0, 1); // Get the latest order
        List<String> lastOrderIdList = orderRepository.findLastOrderIdForUser(userId, pageable);

        if (lastOrderIdList.isEmpty() || lastOrderIdList.get(0) == null) {
            return 1; // Start from 001 if no previous order exists
        }

        String lastOrderId = lastOrderIdList.get(0);
        String[] parts = lastOrderId.split("-"); // Split based on '-'

        if (parts.length < 3) {
            return 1; // Fallback if format is incorrect
        }

        try {
            int lastNumber = Integer.parseInt(parts[2]); // Extract last number
            return lastNumber + 1;
        } catch (NumberFormatException e) {
            return 1; // Handle parsing error
        }
    }


}
