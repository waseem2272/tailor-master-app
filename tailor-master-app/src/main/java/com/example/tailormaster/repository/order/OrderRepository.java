package com.example.tailormaster.repository.order;

import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Fetch orders by customer ID
    List<Order> findByCustomerId(Long customerId);

    // Fetch orders by status
    List<Order> findByStatus(OrderStatus status);
}
