package com.example.tailormaster.dto;

import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.Product;
import com.example.tailormaster.validation.MeasurementValidationGroup;
import com.example.tailormaster.validation.ProductSelectionGroup;
import com.example.tailormaster.validation.measurement.ValidCustomerMeasurement;
import com.example.tailormaster.validation.products.ValidateSelectedProducts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter @Setter
public class CustomerRegistrationDTO {
    private Customer customer;

//    @NotNull(message = "At least one product must be selected.", groups = ProductSelectionGroup.class)
    @NotBlank(message = "At least one product must be selected.", groups = ProductSelectionGroup.class)
    private List<Long> productIds;

    @Valid
    @ValidateSelectedProducts(groups = MeasurementValidationGroup.class)
    private List<Product> products = new ArrayList<>();
    @ValidCustomerMeasurement(groups = MeasurementValidationGroup.class)
    private Map<Long, CustomerMeasurement> customerMeasurements = new HashMap<>();

    public void addCustomerMeasurement(Long productId, CustomerMeasurement measurement) {
        customerMeasurements.put(productId, measurement);
    }
}