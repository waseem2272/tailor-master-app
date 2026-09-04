package com.example.tailormaster.repository.customer;

import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @Query("""
    SELECT c FROM Customer c 
    WHERE c.user = :user 
      AND (LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%')) 
           OR LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<Customer> searchByUserAndFullNameOrPhoneNumber(
            @Param("user") User user,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
    SELECT c FROM Customer c
    WHERE c.user = :user
      AND (LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')))
      AND c.createdAt BETWEEN :startDate AND :endDate
    """)
    Page<Customer> searchByUserAndSearchAndCreatedAtBetween(
            @Param("user") User user,
            @Param("search") String search,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("""
    SELECT c FROM Customer c
    WHERE c.user = :user
      AND c.createdAt BETWEEN :startDate AND :endDate
    """)
    Page<Customer> findByUserAndCreatedAtBetweenCustom(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    Optional<Customer> findByIdAndUser(Long id, User currentUser);

    void deleteByIdAndUser(Long id, User currentUser);

    long countByUser(User currentUser);

    Page<Customer> findAllByUser(User currentUser, Pageable pageable);

    @Query("SELECT c FROM Customer c " +
            "LEFT JOIN FETCH c.measurements m " +
            "LEFT JOIN FETCH m.product p " +
            "LEFT JOIN FETCH m.field f " +
            "WHERE c.id = :id AND c.user = :user")
    Optional<Customer> findByIdWithMeasurements(@Param("id") Long id, User user);

}
