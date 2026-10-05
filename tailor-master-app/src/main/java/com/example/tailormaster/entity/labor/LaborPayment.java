package com.example.tailormaster.entity.labor;

import com.example.tailormaster.entity.BaseEntity;
import com.example.tailormaster.enums.LaborPaymentType;
import com.example.tailormaster.enums.PaymentType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LaborPayment extends BaseEntity {

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "labor_id", nullable = false)
    private Labor labor;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LaborPaymentType paymentType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    private String remarks;
}
