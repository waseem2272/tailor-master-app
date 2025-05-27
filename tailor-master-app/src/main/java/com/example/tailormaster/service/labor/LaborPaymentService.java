package com.example.tailormaster.service.labor;

import com.example.tailormaster.entity.labor.LaborPayment;
import com.example.tailormaster.repository.labor.LaborPaymentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@AllArgsConstructor
public class LaborPaymentService {

    private final LaborPaymentRepository laborPaymentRepository;

    public LaborPayment addPayment(LaborPayment payment) {
        return laborPaymentRepository.save(payment);
    }

    public List<LaborPayment> getPaymentsForLabor(Long laborId) {
        return laborPaymentRepository.findByLaborId(laborId);
    }

    public List<LaborPayment> getPaymentsInDateRange(LocalDate start, LocalDate end) {
        return laborPaymentRepository.findByPaymentDateBetween(start, end);
    }

    public BigDecimal getTotalPaymentsThisMonth() {
        return laborPaymentRepository.getTotalLaborPaymentsThisMonth();
    }
}
