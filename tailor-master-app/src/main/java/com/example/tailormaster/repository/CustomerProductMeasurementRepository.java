package com.example.tailormaster.repository;

import com.example.tailormaster.entity.CustomerProductMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerProductMeasurementRepository
        extends JpaRepository<CustomerProductMeasurement, Long> {

    List<CustomerProductMeasurement> findByUserIdAndCustomerId(
            Long userId,
            Long customerId
    );

    Optional<CustomerProductMeasurement> findByUserIdAndCustomerIdAndProductId(
            Long userId,
            Long customerId,
            Long productId
    );
}