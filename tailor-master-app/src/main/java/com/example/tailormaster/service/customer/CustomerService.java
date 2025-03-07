package com.example.tailormaster.service.customer;

import com.example.tailormaster.dto.CustomerRegistrationDTO;
import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.repository.customer.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

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
    public void updateCustomer(CustomerRegistrationDTO registrationDTO, List<Product> selectedProducts) {
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

        customerRepository.save(existingCustomer);
    }


    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }

    public Page<Customer> getAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    public Page<Customer> searchCustomers(String search, Pageable pageable) {
        return customerRepository.findByFullNameContainingIgnoreCaseOrPhoneNumberContaining(search, search, pageable);
    }

    public long countAllCustomers() {
        return customerRepository.count();
    }

}
