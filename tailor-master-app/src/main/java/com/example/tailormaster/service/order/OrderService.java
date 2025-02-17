package com.example.tailormaster.service.order;

import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.OrderStatus;
import com.example.tailormaster.repository.order.OrderRepository;
import lombok.AllArgsConstructor;
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
}
