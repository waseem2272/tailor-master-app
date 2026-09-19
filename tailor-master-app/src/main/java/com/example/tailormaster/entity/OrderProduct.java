package com.example.tailormaster.entity;

import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.enums.FabricSource;
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

    /**
     * CUSTOMER = Customer brought their own fabric
     * SHOP = Fabric will be consumed from shop inventory
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "fabric_source", length = 20)
    private FabricSource fabricSource = FabricSource.CUSTOMER;

    /**
     * Shop inventory fabric selected for this order product.
     * Null when customer provides fabric.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_product_type", nullable = false, length = 20)
    private OrderProductType orderProductType = OrderProductType.TAILORING;

    /**
     * Amount of fabric consumed from shop inventory.
     * Example: 4.50 meters
     */
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
