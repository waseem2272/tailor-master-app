package com.example.tailormaster.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class ProductSelection {
    
    @NotNull(message = "Product ID is required.")
    private Long productId;

    @NotNull(message = "Product price is required.")
    @Min(value = 0, message = "Price cannot be negative.")
    private BigDecimal price;

    @NotNull(message = "Quantity is required.")
    @Min(value = 1, message = "Quantity must be at least 1.")
    private Integer quantity;
}
