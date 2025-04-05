package com.example.tailormaster.repository.customer;

import com.example.tailormaster.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Page<Customer> findByFullNameContainingIgnoreCaseOrPhoneNumberContainingIgnoreCase(
            String fullName, String phoneNumber, Pageable pageable);

    Page<Customer> findByFullNameContainingIgnoreCaseOrPhoneNumberContainingIgnoreCaseAndCreatedAtBetween(
            String fullName, String phoneNumber, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<Customer> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
