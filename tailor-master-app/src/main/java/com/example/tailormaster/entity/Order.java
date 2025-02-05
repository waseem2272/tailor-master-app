package com.example.tailormaster.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "customer_order")
public class Order extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private LocalDate orderDate;
    private LocalDate deliveryDate;
    private BigDecimal extraCharges;
    private String extraChargesDescription;
    private BigDecimal advancePayment;
    private BigDecimal totalPayment;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING; // Default status

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderProduct> orderProducts = new ArrayList<>();

}
