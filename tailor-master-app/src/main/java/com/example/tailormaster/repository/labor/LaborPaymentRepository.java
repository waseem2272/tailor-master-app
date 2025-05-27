package com.example.tailormaster.repository.labor;

import com.example.tailormaster.entity.labor.LaborPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LaborPaymentRepository extends JpaRepository<LaborPayment, Long> {
    List<LaborPayment> findByLaborId(Long laborId);
    List<LaborPayment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM LaborPayment p WHERE p.paymentType = 'DEBIT' AND MONTH(p.paymentDate) = MONTH(CURRENT_DATE) AND YEAR(p.paymentDate) = YEAR(CURRENT_DATE)")
    BigDecimal getTotalLaborPaymentsThisMonth();
}
