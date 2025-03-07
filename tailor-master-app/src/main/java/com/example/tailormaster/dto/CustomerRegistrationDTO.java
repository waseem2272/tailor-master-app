package com.example.tailormaster.dto;

import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.validation.MeasurementValidationGroup;
import com.example.tailormaster.validation.measurement.ValidCustomerMeasurement;
import com.example.tailormaster.validation.products.ValidateSelectedProducts;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter @Setter
public class CustomerRegistrationDTO {

    @Valid
    private Customer customer;

    @Valid
    @ValidateSelectedProducts(groups = MeasurementValidationGroup.class)
    private List<Product> products = new ArrayList<>();
    @ValidCustomerMeasurement(groups = MeasurementValidationGroup.class)
    private Map<Long, CustomerMeasurement> customerMeasurements = new HashMap<>();

    // Store selected product IDs separately for easier pre-selection
    private Set<Long> selectedProductIds = new HashSet<>();

    public void addCustomerMeasurement(Long productId, CustomerMeasurement measurement) {
        customerMeasurements.put(productId, measurement);
    }
}