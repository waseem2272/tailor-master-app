package com.example.tailormaster.repository.labor;

import com.example.tailormaster.entity.labor.LaborPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LaborPaymentRepository extends JpaRepository<LaborPayment, Long> {

    List<LaborPayment> findByLaborId(Long laborId);

    @Query("""
           SELECT COALESCE(SUM(p.amount), 0)
           FROM LaborPayment p
           WHERE p.labor.id = :laborId
           AND p.paymentType = 'WORK'
           """)
    BigDecimal sumWorkByLaborId(@Param("laborId") Long laborId);

    @Query("""
           SELECT COALESCE(SUM(p.amount), 0)
           FROM LaborPayment p
           WHERE p.labor.id = :laborId
           AND p.paymentType = 'SALARY'
           """)
    BigDecimal sumSalaryByLaborId(@Param("laborId") Long laborId);

    @Query("""
           SELECT COALESCE(SUM(p.amount), 0)
           FROM LaborPayment p
           WHERE p.labor.id = :laborId
           AND p.paymentType = 'ADVANCE'
           """)
    BigDecimal sumAdvanceByLaborId(@Param("laborId") Long laborId);

    @Query("""
           SELECT COALESCE(SUM(p.amount), 0)
           FROM LaborPayment p
           WHERE p.labor.id = :laborId
           AND p.paymentType IN ('SALARY', 'ADVANCE')
           """)
    BigDecimal sumTotalPaidByLaborId(@Param("laborId") Long laborId);

    @Query("""
           SELECT
               COALESCE(SUM(CASE
                   WHEN p.paymentType = 'WORK' THEN p.amount
                   ELSE 0
               END), 0)
               -
               COALESCE(SUM(CASE
                   WHEN p.paymentType IN ('SALARY', 'ADVANCE') THEN p.amount
                   ELSE 0
               END), 0)
           FROM LaborPayment p
           WHERE p.labor.id = :laborId
           """)
    BigDecimal findLaborBalance(@Param("laborId") Long laborId);
}
