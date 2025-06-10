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
    List<LaborPayment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);

//    @Query("SELECT SUM(p.amount) FROM LaborPayment p WHERE p.labor.id = :laborId")
//    Optional<BigDecimal> sumByLaborId(@Param("laborId") Long laborId);

    @Query("SELECT SUM(COALESCE(p.workAmount, 0)) FROM LaborPayment p WHERE p.labor.id = :laborId AND p.paymentType = 'REGULAR' ")
    Optional<BigDecimal> sumRegularByLaborId(@Param("laborId") Long laborId);

    @Query("SELECT (SUM(COALESCE(p.workAmount, 0)) + SUM(COALESCE(p.deductions, 0))) FROM LaborPayment p WHERE p.labor.id = :laborId AND p.paymentType IN ('PAID', 'ADVANCE', 'BORROW') ")
    Optional<BigDecimal> sumPaidByLaborId(@Param("laborId") Long laborId);

    @Query("SELECT SUM(COALESCE(lp.deductions, 0)) FROM LaborPayment lp WHERE lp.labor.id = :laborId AND lp.paymentType = 'ADVANCE' ")
    Optional<BigDecimal> sumAdvanceByLaborId(@Param("laborId") Long laborId);

    @Query("SELECT SUM(COALESCE(p.deductions, 0)) FROM LaborPayment p WHERE p.labor.id = :laborId AND p.paymentType = 'BORROW' ")
    Optional<BigDecimal> sumBorrowByLaborId(@Param("laborId") Long laborId);

    @Query("SELECT lp.labor.id, SUM(COALESCE(lp.workAmount, 0)), SUM(COALESCE(lp.deductions, 0)) " +
            "FROM LaborPayment lp GROUP BY lp.labor.id")
    List<Object[]> findLaborBalances();

    @Query("SELECT" +
            " SUM(CASE WHEN lp.paymentType = 'REGULAR' THEN COALESCE(lp.workAmount, 0) ELSE 0 END) -" +
            " SUM(CASE WHEN lp.paymentType = 'PAID' THEN COALESCE(lp.workAmount, 0) ELSE 0 END) +" +
            " SUM(CASE WHEN lp.paymentType IN ('ADVANCE', 'BORROW') THEN COALESCE(lp.deductions, 0) ELSE 0 END) AS balance" +
            " FROM LaborPayment lp WHERE lp.labor.id = :laborId" +
            " GROUP BY lp.labor.id")
    BigDecimal findLaborBalance(Long laborId);
}
