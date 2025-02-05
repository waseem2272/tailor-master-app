package com.example.tailormaster.repository.customer;

import com.example.tailormaster.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // Search for customers by name or phone number
    Page<Customer> findByFullNameContainingIgnoreCaseOrPhoneNumberContaining(
            String fullName, String phoneNumber, Pageable pageable
    );
}
