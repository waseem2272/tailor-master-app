package com.example.tailormaster.entity.labor;

import com.example.tailormaster.entity.BaseEntity;
import com.example.tailormaster.enums.PaymentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LaborPayment extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "labor_id")
    private Labor labor;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;    // DEBIT or CREDIT

    @Column(nullable = false)
    private BigDecimal amount;

    private String remarks;
}
