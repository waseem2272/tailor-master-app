package com.example.tailormaster.service.labor;

import com.example.tailormaster.entity.labor.LaborPayment;
import com.example.tailormaster.repository.labor.LaborPaymentRepository;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class LaborPaymentService {

    private static final Logger logger = LogManager.getLogger(LaborPaymentService.class);

    private final LaborPaymentRepository laborPaymentRepository;

    public List<LaborPayment> getPaymentsForLabor(Long laborId) {
        return laborPaymentRepository.findByLaborId(laborId);
    }

    //    public BigDecimal getTotalPaidByLaborId(Long laborId) {
//        return laborPaymentRepository.sumByLaborId(laborId).orElse(BigDecimal.ZERO);
//    }

    public BigDecimal getTotalRegularPaidByLaborId(Long laborId) {
        return laborPaymentRepository.sumRegularByLaborId(laborId).orElse(BigDecimal.ZERO);
    }

    public BigDecimal getTotalPaidByLaborId(Long laborId) {
        return laborPaymentRepository.sumPaidByLaborId(laborId).orElse(BigDecimal.ZERO);
    }

    public BigDecimal getTotalAdvancePaidByLaborId(Long laborId) {
        return laborPaymentRepository.sumAdvanceByLaborId(laborId).orElse(BigDecimal.ZERO);
    }

    public BigDecimal getTotalBorrowPaidByLaborId(Long laborId) {
        return laborPaymentRepository.sumBorrowByLaborId(laborId).orElse(BigDecimal.ZERO);
    }

    public LaborPayment savePayment(BigDecimal amount, LaborPayment payment) {
        try {
            switch (payment.getPaymentType()) {
                case REGULAR:
                case PAID:
                    payment.setWorkAmount(amount);
                    payment.setDeductions(BigDecimal.ZERO);
                    break;
                case ADVANCE:
                case BORROW:
                    payment.setDeductions(amount);
                    payment.setWorkAmount(BigDecimal.ZERO);
                    break;
            }
            return laborPaymentRepository.save(payment);
        } catch (Exception e) {
            logger.error("Error saving labor payment: {}", e.getMessage(), e);
            throw new RuntimeException("Error saving labor payment:", e);
        }
    }

    public Map<Long, BigDecimal> getLaborBalances() {
        List<LaborPayment> allPayments = laborPaymentRepository.findAll();
        Map<Long, BigDecimal> balanceMap = new HashMap<>();

        for (LaborPayment lp : allPayments) {
            Long laborId = lp.getLabor().getId();

            BigDecimal regularPaid = getTotalRegularPaidByLaborId(laborId);

            BigDecimal totalPaid = getTotalPaidByLaborId(laborId);
            BigDecimal remainingBalance = regularPaid.subtract(totalPaid);

//            BigDecimal work = lp.getWorkAmount() != null ? lp.getWorkAmount() : BigDecimal.ZERO;
//            BigDecimal ded = lp.getDeductions() != null ? lp.getDeductions() : BigDecimal.ZERO;
//            BigDecimal paid = lp.getWorkAmount() != null ? lp.getWorkAmount() : BigDecimal.ZERO;
//            BigDecimal net = paid.add(ded).subtract(work);
            balanceMap.put(laborId, remainingBalance);
        }

        return balanceMap;
    }

    public BigDecimal getLaborBalance(Long laborId) {
        return this.laborPaymentRepository.findLaborBalance(laborId);
    }

    public Optional<LaborPayment> getLaborPayment(Long laborPaymentId) {
        return laborPaymentRepository.findById(laborPaymentId);
    }

    public void deleteById(Long id) {
        laborPaymentRepository.deleteById(id);
    }
}
