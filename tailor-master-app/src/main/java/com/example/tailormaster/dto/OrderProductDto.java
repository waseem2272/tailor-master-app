package com.example.tailormaster.dto;

import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.enums.FabricSource;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderProductDto extends Product {
    private Long productId;

    private String silaiType;

    private BigDecimal silaiAmount;

    private Integer quantity;

    private BigDecimal amount;

    private String additionalNotes;
    private FabricSource fabricSource = FabricSource.CUSTOMER;

    private Long inventoryItemId;

    private BigDecimal fabricQuantity;
}
