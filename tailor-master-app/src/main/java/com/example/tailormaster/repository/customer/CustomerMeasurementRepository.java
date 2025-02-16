package com.example.tailormaster.repository.customer;

import com.example.tailormaster.entity.CustomerMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerMeasurementRepository extends JpaRepository<CustomerMeasurement, Long> {
    List<CustomerMeasurement> findByCustomerId(Long customerId); // Correct method
}
