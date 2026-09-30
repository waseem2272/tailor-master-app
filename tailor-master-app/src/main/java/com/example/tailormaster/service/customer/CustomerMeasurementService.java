package com.example.tailormaster.service.customer;

import com.example.tailormaster.dto.CustomerMeasurementFieldResponse;
import com.example.tailormaster.dto.CustomerMeasurementResponse;
import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.CustomerProductMeasurement;
import com.example.tailormaster.entity.ProductMeasurementField;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.repository.CustomerProductMeasurementRepository;
import com.example.tailormaster.repository.ProductMeasurementFieldRepository;
import com.example.tailormaster.repository.customer.CustomerMeasurementRepository;
import com.example.tailormaster.repository.customer.CustomerRepository;
import com.example.tailormaster.repository.product.ProductRepository;
import com.example.tailormaster.util.AuthenticatedUserService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CustomerMeasurementService {

    private final CustomerMeasurementRepository measurementRepository;
    private final CustomerProductMeasurementRepository customerProductMeasurementRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ProductMeasurementFieldRepository fieldRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public Long getCurrentUserId() {
        return authenticatedUserService.getCurrentUser().getId();
    }

    public List<CustomerMeasurement> getCustomerMeasurement(Long customerId) {
        return measurementRepository.findByUserIdAndCustomerId(
                getCurrentUserId(),
                customerId
        );
    }

    public List<CustomerMeasurement> getMeasurement(Long customerId, Long productId) {
        return measurementRepository.findByUserIdAndCustomerIdAndProductIdOrderByCreatedAtDesc(
                getCurrentUserId(),
                customerId,
                productId
        );
    }

    @Transactional
    public void saveMeasurements(Long customerId,
                                 Long productId,
                                 Map<Long, String> measurements,
                                 String notes) {

        Long userId = getCurrentUserId();

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        List<ProductMeasurementField> fields =
                fieldRepository.findByUserIdAndProductIdOrderByIdAsc(
                        userId,
                        productId
                );

        Map<Long, ProductMeasurementField> fieldMap = fields.stream()
                .collect(Collectors.toMap(
                        ProductMeasurementField::getId,
                        field -> field
                ));

        CustomerProductMeasurement customerProductMeasurement =
                new CustomerProductMeasurement();

        customerProductMeasurement.setCustomer(customer);
        customerProductMeasurement.setProduct(product);
        customerProductMeasurement.setUser(authenticatedUserService.getCurrentUser());
        customerProductMeasurement.setNotes(
                notes != null ? notes.trim() : null
        );

        customerProductMeasurement =
                customerProductMeasurementRepository.save(customerProductMeasurement);

        for (Map.Entry<Long, String> entry : measurements.entrySet()) {

            Long fieldId = entry.getKey();
            String value = entry.getValue();

            if (value == null || value.isBlank()) {
                continue;
            }

            ProductMeasurementField field = fieldMap.get(fieldId);

            if (field == null) {
                continue;
            }

            CustomerMeasurement measurement = new CustomerMeasurement();
            measurement.setCustomer(customer);
            measurement.setProduct(product);
            measurement.setField(field);
            measurement.setCustomerProductMeasurement(customerProductMeasurement);
            measurement.setUser(authenticatedUserService.getCurrentUser());
            measurement.setValue(value.trim());

            measurementRepository.save(measurement);
        }
    }

    public List<CustomerProductMeasurement> getCustomerProductMeasurements(Long customerId) {
        return customerProductMeasurementRepository.findByUserIdAndCustomerId(
                getCurrentUserId(),
                customerId
        );
    }

    public CustomerMeasurementResponse getCustomerProductMeasurement(Long id) {
        Long userId = getCurrentUserId();
        CustomerProductMeasurement measurement = customerProductMeasurementRepository.findByUserIdAndId(userId, id)
                .orElseThrow(() -> new IllegalArgumentException("Measurement not found"));
        List<CustomerMeasurement> customerMeasurements =
                measurementRepository.findByUserIdAndCustomerProductMeasurementIdOrderByIdAsc(userId, id);
        List<CustomerMeasurementFieldResponse> fields = customerMeasurements.stream()
                .map(item -> new CustomerMeasurementFieldResponse(
                        item.getField().getId(),
                        item.getField().getFieldName(),
                        item.getField().getFieldType(),
                        item.getValue(),
                        item.getField().getOptions()
                ))
                .collect(Collectors.toList());
        return new CustomerMeasurementResponse(
                measurement.getId(),
                measurement.getCustomer().getId(),
                measurement.getProduct().getId(),
                measurement.getNotes(),
                fields
        );
    }

    @Transactional
    public void updateCustomerProductMeasurement(Long measurementId,
                                                 Map<Long, String> measurements,
                                                 String notes) {

        Long userId = getCurrentUserId();

        CustomerProductMeasurement customerProductMeasurement =
                customerProductMeasurementRepository.findByUserIdAndId(userId, measurementId)
                        .orElseThrow(() -> new IllegalArgumentException("Measurement not found"));

        customerProductMeasurement.setNotes(notes != null ? notes.trim() : null);
        customerProductMeasurementRepository.save(customerProductMeasurement);

        List<CustomerMeasurement> existingMeasurements =
                measurementRepository.findByUserIdAndCustomerProductMeasurementIdOrderByIdAsc(
                        userId,
                        measurementId
                );

        Map<Long, CustomerMeasurement> existingMeasurementMap =
                existingMeasurements.stream()
                        .collect(Collectors.toMap(
                                item -> item.getField().getId(),
                                item -> item
                        ));

        for (Map.Entry<Long, String> entry : measurements.entrySet()) {

            Long fieldId = entry.getKey();
            String value = entry.getValue();

            CustomerMeasurement measurement = existingMeasurementMap.get(fieldId);

            if (measurement == null) {
                continue;
            }

            measurement.setValue(value != null ? value.trim() : null);
            measurementRepository.save(measurement);
        }
    }
}