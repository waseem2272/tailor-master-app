package com.example.tailormaster.service.customer;

import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.repository.customer.CustomerMeasurementRepository;
import org.springframework.stereotype.Service;

@Service
public class CustomerMeasurementService {
    private final CustomerMeasurementRepository measurementRepository;

    public CustomerMeasurementService(CustomerMeasurementRepository measurementRepository) {
        this.measurementRepository = measurementRepository;
    }

    public void saveMeasurement(CustomerMeasurement measurement) {
        measurementRepository.save(measurement);
    }
}
