package com.example.tailormaster.entity;

import com.example.tailormaster.enums.OrderStatus;
import com.example.tailormaster.enums.PickupStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private BigDecimal advancePayment;
    private BigDecimal duePayment;
    private BigDecimal totalProductAmount;

    private String orderId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING; // Default status

    private String cabinetNo; // Add this field

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderProduct> orderProducts = new ArrayList<>();

    // ✅ New fields for pickup and due management
    @Enumerated(EnumType.STRING)
    private PickupStatus pickupStatus = PickupStatus.NOT_PICKED_UP;  // default value

    private LocalDateTime pickupDate;   // store date & time of pickup

    private Boolean pickedUpWithDue = false;   // flag if picked up with due

    private BigDecimal outstandingDueAmount = BigDecimal.ZERO;  // snapshot of due at pickup time

    private BigDecimal paidAmount = BigDecimal.ZERO;

}
