package com.example.tailormaster.service.customer;

import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.repository.customer.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }

    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public void updateCustomer(Long id, Customer updatedCustomer) {
        Customer existingCustomer = getCustomerById(id);
        existingCustomer.setFullName(updatedCustomer.getFullName());
//        existingCustomer.setFatherName(updatedCustomer.getFatherName());
//        existingCustomer.setSurname(updatedCustomer.getSurname());
        existingCustomer.setPhoneNumber(updatedCustomer.getPhoneNumber());
//        existingCustomer.setChest(updatedCustomer.getChest());
//        existingCustomer.setWaist(updatedCustomer.getWaist());
//        existingCustomer.setHips(updatedCustomer.getHips());
//        existingCustomer.setSleeveLength(updatedCustomer.getSleeveLength());
//        existingCustomer.setShoulder(updatedCustomer.getShoulder());
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
