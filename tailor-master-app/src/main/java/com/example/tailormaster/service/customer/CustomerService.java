package com.example.tailormaster.service.customer;

import com.example.tailormaster.dto.CustomerDTO;
import com.example.tailormaster.dto.CustomerRegistrationDTO;
import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.repository.customer.CustomerRepository;
import com.example.tailormaster.service.product.ProductService;
import com.example.tailormaster.util.AuthenticatedUserService;
import com.example.tailormaster.util.ThymeleafUtil;
import com.example.tailormaster.validation.Utility;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@AllArgsConstructor
public class CustomerService {

    private static final Logger logger = LogManager.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final ProductService productService;
    private final AuthenticatedUserService authenticatedUserService;

    public Customer getCustomerById(Long id) {
        logger.debug("Fetching customer by ID: {}", id);
        return customerRepository.findByIdAndUser(id, authenticatedUserService.getCurrentUser()).orElse(null);
    }

    public Customer createCustomer(Customer customer) {
        logger.info("Creating customer: {}", customer.getFullName());
        customer.setUser(authenticatedUserService.getCurrentUser());
        Customer savedCustomer = customerRepository.save(customer);
        logger.debug("Customer created with ID: {}", savedCustomer.getId());
        return savedCustomer;
    }

    @Transactional
    public Customer updateCustomer(CustomerRegistrationDTO registrationDTO, Long[] productIds) {
        Long customerId = registrationDTO.getCustomer().getId();

        Set<Product> products = new HashSet<>();

        for (Long productId : productIds) {
            Product product = productService.getProductById(productId);
            products.add(product);
        }

        logger.info("Updating customer with ID: {}", customerId);

        Customer existingCustomer = customerRepository.findByIdAndUser(customerId, authenticatedUserService.getCurrentUser())
                .orElseThrow(() -> {
                    logger.error("Customer not found with ID: {}", customerId);
                    return new IllegalArgumentException("Customer not found");
                });

        existingCustomer.setFullName(registrationDTO.getCustomer().getFullName());
        existingCustomer.setPhoneNumber(registrationDTO.getCustomer().getPhoneNumber());
        existingCustomer.getMeasurements().clear();

        for (Product product : products) {
            CustomerMeasurement measurement = new CustomerMeasurement();
                measurement.setCustomer(existingCustomer);
                measurement.setProduct(product);

            CustomerMeasurement measurement1 = Utility.populateCustomerMeasurement(measurement, registrationDTO);
            existingCustomer.getMeasurements().add(measurement1);
        }
        existingCustomer.setUser(authenticatedUserService.getCurrentUser());
        Customer updatedCustomer = customerRepository.save(existingCustomer);
        logger.debug("Customer updated successfully: {}", updatedCustomer.getId());
        return updatedCustomer;
    }

    public void deleteCustomer(Long id) {
        logger.info("Deleting customer with ID: {}", id);
        customerRepository.deleteByIdAndUser(id, authenticatedUserService.getCurrentUser());
        logger.debug("Customer deleted successfully");
    }

    public Map<String, Object> getCustomersData(int draw, int start, int length, String searchValue,
                                                Integer columnIndex, String sortDirection,
                                                LocalDate startDate, LocalDate endDate) {
        Map<String, Object> response = new HashMap<>();
        try {
            logger.debug("Fetching customers for DataTables: page={}, length={}, search={}, date range=[{} - {}]",
                    start / length, length, searchValue, startDate, endDate);

            int page = start / length;
            Page<Customer> customerPage = getCustomersForDataTables(page, length, searchValue, columnIndex, sortDirection, startDate, endDate);

            List<CustomerDTO> customerDTOs = customerPage.getContent().stream()
                    .map(customer -> new CustomerDTO(customer, new ThymeleafUtil()))
                    .toList();

            response.put("draw", draw);
            response.put("recordsTotal", getTotalCustomerCount());
            response.put("recordsFiltered", customerPage.getTotalElements());
            response.put("data", customerDTOs);

            logger.debug("Customer data fetched: {} records", customerDTOs.size());
        } catch (Exception e) {
            logger.error("Error retrieving customers: {}", e.getMessage(), e);
            throw new RuntimeException("Error retrieving customers from the database.", e);
        }

        return response;
    }

    public Page<Customer> getCustomersForDataTables(int page, int size, String search, Integer columnIndex,
                                                    String sortDirection, LocalDate startDate, LocalDate endDate) {
        try {
            logger.debug("Preparing pageable customer data. Page: {}, Size: {}, Search: '{}'", page, size, search);
            Pageable pageable;
            String[] columns = {"id", "fullName", "phoneNumber", "createdAt"};
            String sortBy = (columnIndex != null && columnIndex >= 0 && columnIndex < columns.length) ? columns[columnIndex] : "createdAt";
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
//            pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<Customer> result;
            if (search != null && !search.isEmpty() && startDate != null && endDate != null) {
                result = customerRepository.searchByUserAndSearchAndCreatedAtBetween(
                        authenticatedUserService.getCurrentUser(),
                        search,
                        startDate.atStartOfDay(),
                        endDate.atTime(23, 59, 59),
                        pageable
                );

            } else if (search != null && !search.isEmpty()) {
                result = customerRepository.searchByUserAndFullNameOrPhoneNumber(authenticatedUserService.getCurrentUser(), search, pageable);
            } else if (startDate != null && endDate != null) {
                result = customerRepository.findByUserAndCreatedAtBetweenCustom(
                        authenticatedUserService.getCurrentUser(),
                        startDate.atStartOfDay(),
                        endDate.atTime(23, 59, 59),
                        pageable
                );

            } else {
                result = customerRepository.findAllByUser(authenticatedUserService.getCurrentUser(), pageable);
            }

            logger.debug("Customer page fetched: {} items", result.getNumberOfElements());
            return result;

        } catch (Exception e) {
            logger.error("Error retrieving customer page: {}", e.getMessage(), e);
            throw new RuntimeException("Error retrieving customers from the database.", e);
        }
    }

    public long getTotalCustomerCount() {
        long count = customerRepository.countByUser(authenticatedUserService.getCurrentUser());
        logger.debug("Total customer count: {}", count);
        return count;
    }
}
