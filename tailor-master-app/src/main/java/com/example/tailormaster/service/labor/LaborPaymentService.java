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

    public BigDecimal getTotalWorkByLaborId(Long laborId) {
        return laborPaymentRepository.sumWorkByLaborId(laborId);
    }

    public BigDecimal getTotalSalaryByLaborId(Long laborId) {
        return laborPaymentRepository.sumSalaryByLaborId(laborId);
    }

    public BigDecimal getTotalAdvanceByLaborId(Long laborId) {
        return laborPaymentRepository.sumAdvanceByLaborId(laborId);
    }

    public BigDecimal getTotalPaidByLaborId(Long laborId) {
        return laborPaymentRepository.sumTotalPaidByLaborId(laborId);
    }

    public BigDecimal getLaborBalance(Long laborId) {
        return laborPaymentRepository.findLaborBalance(laborId);
    }

    public LaborPayment savePayment(BigDecimal amount, LaborPayment payment) {
        try {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Payment amount must be greater than zero.");
            }

            if (payment.getPaymentType() == null) {
                throw new IllegalArgumentException("Payment type is required.");
            }

            payment.setAmount(amount);

            return laborPaymentRepository.save(payment);

        } catch (Exception e) {
            logger.error("Error saving labor payment: {}", e.getMessage(), e);
            throw new RuntimeException("Error saving labor payment.", e);
        }
    }

    public Map<Long, BigDecimal> getLaborBalances() {
        List<LaborPayment> allPayments = laborPaymentRepository.findAll();
        Map<Long, BigDecimal> balanceMap = new HashMap<>();

        for (LaborPayment payment : allPayments) {
            Long laborId = payment.getLabor().getId();

            BigDecimal balance = getLaborBalance(laborId);

            balanceMap.put(laborId, balance);
        }

        return balanceMap;
    }

    public Optional<LaborPayment> getLaborPayment(Long laborPaymentId) {
        return laborPaymentRepository.findById(laborPaymentId);
    }

    public void deleteById(Long id) {
        laborPaymentRepository.deleteById(id);
    }
}
