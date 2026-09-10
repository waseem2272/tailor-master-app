package com.example.tailormaster.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "order_inventory_usage",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_order_product_inventory_usage",
                        columnNames = "order_product_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderInventoryUsage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    private Order order;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_product_id", nullable = false, unique = true)
    @ToString.Exclude
    private OrderProduct orderProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    @ToString.Exclude
    private InventoryItem inventoryItem;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(length = 20)
    private String unit;

    @Column(nullable = false)
    private boolean reversed = false;

    private LocalDateTime reversedAt;
}