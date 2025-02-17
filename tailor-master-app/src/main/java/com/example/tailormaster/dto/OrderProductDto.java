package com.example.tailormaster.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class OrderProductDto {
    @NotNull(message = "Product ID is required.")
    private Long productId;
    @Min(value = 1, message = "Quantity must be at least 1.")
    private int quantity;
}
