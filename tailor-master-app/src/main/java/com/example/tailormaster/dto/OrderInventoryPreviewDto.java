package com.example.tailormaster.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderInventoryPreviewDto {
    private BigDecimal quantity;
    private BigDecimal totalAmount;
    private BigDecimal averageUnitPrice;
}