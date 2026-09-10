package com.example.tailormaster.repository;

import com.example.tailormaster.entity.OrderInventoryUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderInventoryUsageRepository
        extends JpaRepository<OrderInventoryUsage, Long> {

    boolean existsByOrderProductId(Long orderProductId);

    List<OrderInventoryUsage> findByOrderId(Long orderId);

    List<OrderInventoryUsage> findByOrderIdAndReversedFalse(Long orderId);
}