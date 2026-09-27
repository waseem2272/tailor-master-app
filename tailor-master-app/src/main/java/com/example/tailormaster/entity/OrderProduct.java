package com.example.tailormaster.entity;

import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.enums.FabricSource;
import com.example.tailormaster.enums.OrderProductStatus;
import com.example.tailormaster.enums.OrderProductType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class OrderProduct extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private int quantity;

    private BigDecimal silaiAmount;

    private BigDecimal subtotal;

    private String silaiType;

    @Column(columnDefinition = "TEXT")
    private String additionalNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "fabric_source", length = 20)
    private FabricSource fabricSource = FabricSource.CUSTOMER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_product_type", nullable = false, length = 50)
    private OrderProductType orderProductType = OrderProductType.TAILORING;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private OrderProductStatus status;

    @Column(name = "alteration_required", nullable = false)
    private boolean alterationRequired = false;

    @Column(name = "alteration_fee", precision = 12, scale = 2)
    private BigDecimal alterationFee = BigDecimal.ZERO;

    @Column(name = "fabric_quantity", precision = 12, scale = 2)
    private BigDecimal fabricQuantity;

    @Column(name = "fabric_unit_price", precision = 12, scale = 2)
    private BigDecimal fabricUnitPrice;

    @Column(name = "fabric_amount", precision = 12, scale = 2)
    private BigDecimal fabricAmount;

    @Column(name = "inventory_unit_price", precision = 12, scale = 2)
    private BigDecimal inventoryUnitPrice;

    @Column(name = "inventory_amount", precision = 12, scale = 2)
    private BigDecimal inventoryAmount;
}
