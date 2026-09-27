package com.example.tailormaster.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_stock_batch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class InventoryStockBatch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    @ToString.Exclude
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(name = "batch_code", nullable = false, length = 100)
    private String batchCode;

    @Column(name = "received_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal receivedQuantity = BigDecimal.ZERO;

    @Column(name = "remaining_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal remainingQuantity = BigDecimal.ZERO;

    @Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice = BigDecimal.ZERO;

    @Column(name = "received_date", nullable = false)
    private LocalDateTime receivedDate;

    @Column(nullable = false)
    private Boolean active = true;
}