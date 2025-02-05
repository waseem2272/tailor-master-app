package com.example.tailormaster.repository.customer;

import com.example.tailormaster.entity.CustomerMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerMeasurementRepository extends JpaRepository<CustomerMeasurement, Long> {
}
