package com.example.tailormaster.repository;

import com.example.tailormaster.entity.OrderInventoryUsage;
import com.example.tailormaster.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderInventoryUsageRepository
        extends JpaRepository<OrderInventoryUsage, Long> {

    boolean existsByUserAndOrderProductId(User user, Long orderProductId);

    List<OrderInventoryUsage> findByUserAndOrderId(User user, Long orderId);

    List<OrderInventoryUsage> findByUserAndOrderIdAndReversedFalse(
            User user,
            Long orderId
    );
}