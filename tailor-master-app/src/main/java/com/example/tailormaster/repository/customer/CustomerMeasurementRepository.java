package com.example.tailormaster.repository.customer;

import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerMeasurementRepository extends JpaRepository<CustomerMeasurement, Long> {
    List<CustomerMeasurement> findByUserIdAndCustomerId(
            Long userId,
            Long customerId
    );

    List<CustomerMeasurement> findByUserIdAndCustomerIdAndProductIdOrderByCreatedAtDesc(
            Long userId,
            Long customerId,
            Long productId
    );

    void deleteByUserIdAndCustomerId(
            Long userId,
            Long customerId
    );
}
