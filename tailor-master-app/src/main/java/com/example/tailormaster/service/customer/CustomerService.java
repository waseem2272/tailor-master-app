package com.example.tailormaster.service.customer;

import com.example.tailormaster.dto.CustomerDTO;
import com.example.tailormaster.dto.CustomerRegistrationDTO;
import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.repository.customer.CustomerRepository;
import com.example.tailormaster.util.ThymeleafUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id).orElse(null);
    }

    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(CustomerRegistrationDTO registrationDTO, List<Product> selectedProducts) {
        Long customerId = registrationDTO.getCustomer().getId();
        Customer existingCustomer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        // Update customer information
        existingCustomer.setFullName(registrationDTO.getCustomer().getFullName());
        existingCustomer.setPhoneNumber(registrationDTO.getCustomer().getPhoneNumber());

        // Update Products
        existingCustomer.getMeasurements().clear();

        for (Product product : selectedProducts) {
            CustomerMeasurement measurement = registrationDTO.getCustomerMeasurements().get(product.getId());
            if (measurement != null) {
                measurement.setCustomer(existingCustomer);
                measurement.setProduct(product);
                existingCustomer.getMeasurements().add(measurement);
            }
        }

        return customerRepository.save(existingCustomer);
    }

    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }

    public Map<String, Object> getCustomersData(int draw, int start, int length, String searchValue,
                                                Integer columnIndex, String sortDirection,
                                                LocalDate startDate, LocalDate endDate) {
        Map<String, Object> response = new HashMap<>();
        try {
            int page = start / length;
            Page<Customer> customerPage = getCustomersForDataTables(page, length, searchValue, columnIndex, sortDirection, startDate, endDate);

            List<CustomerDTO> customerDTOs = customerPage.getContent().stream()
                    .map(customer -> new CustomerDTO(customer, new ThymeleafUtil()))
                    .toList();

            response.put("draw", draw);
            response.put("recordsTotal", getTotalCustomerCount());
            response.put("recordsFiltered", customerPage.getTotalElements());
            response.put("data", customerDTOs);

        } catch (Exception e) {
            throw new RuntimeException("Error retrieving customers from the database.", e);
        }

        return response;
    }

    public Page<Customer> getCustomersForDataTables(int page, int size, String search, Integer columnIndex,
                                                    String sortDirection, LocalDate startDate, LocalDate endDate) {
        try {
            Pageable pageable;

            // Include createdAt as a sortable column
            String[] columns = {"id", "fullName", "phoneNumber", "createdAt"};
            String sortBy = (columnIndex != null && columnIndex >= 0 && columnIndex < columns.length) ? columns[columnIndex] : "createdAt"; // Default to createdAt
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
            pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            if (search != null && !search.isEmpty() && startDate != null && endDate != null) {
                return customerRepository.findByFullNameContainingIgnoreCaseOrPhoneNumberContainingIgnoreCaseAndCreatedAtBetween(
                        search, search, startDate.atStartOfDay(), endDate.atTime(23, 59, 59), pageable);
            } else if (search != null && !search.isEmpty()) {
                return customerRepository.findByFullNameContainingIgnoreCaseOrPhoneNumberContainingIgnoreCase(search, search, pageable);
            } else if (startDate != null && endDate != null) {
                return customerRepository.findByCreatedAtBetween(startDate.atStartOfDay(), endDate.atTime(23, 59, 59), pageable);
            }

            return customerRepository.findAll(pageable);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving customers from the database.", e);
        }
    }


    public long getTotalCustomerCount() {
        return customerRepository.count();
    }


}
