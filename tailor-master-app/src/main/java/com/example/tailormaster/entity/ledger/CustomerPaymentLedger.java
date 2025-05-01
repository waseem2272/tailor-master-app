package com.example.tailormaster.entity.ledger;

import com.example.tailormaster.entity.BaseEntity;
import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.Order;
import com.example.tailormaster.enums.PaymentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@ToString
@Getter
@Setter
@NoArgsConstructor
@Entity
public class CustomerPaymentLedger extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    private Order order;

    private LocalDate paymentDate;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;    // DEBIT or CREDIT

    private String remarks;

    public CustomerPaymentLedger(Customer customer, Order order, BigDecimal amount, LocalDate paymentDate, PaymentType paymentType, String remarks) {
        this.customer = customer;
        this.order = order;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentType = paymentType;
        this.remarks = remarks;
    }
}
