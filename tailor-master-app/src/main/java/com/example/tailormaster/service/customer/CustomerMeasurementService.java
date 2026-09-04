package com.example.tailormaster.service.customer;

import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.repository.customer.CustomerMeasurementRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerMeasurementService {
    private final CustomerMeasurementRepository measurementRepository;

    public CustomerMeasurementService(CustomerMeasurementRepository measurementRepository) {
        this.measurementRepository = measurementRepository;
    }

    public void saveMeasurement(CustomerMeasurement measurement) {
        measurementRepository.save(measurement);
    }

    public List<CustomerMeasurement> getCustomerMeasurement(Long customerId) {
        return measurementRepository.findByCustomerId(customerId);
    }

    public List<CustomerMeasurement> getMeasurement(Long customerId, Long productId) {
        return measurementRepository.findByCustomerIdAndProductIdOrderByCreatedAtDesc(customerId, productId);
    }

}
