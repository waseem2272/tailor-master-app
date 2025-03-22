package com.example.tailormaster.dto;

import com.example.tailormaster.entity.product.Product;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderProductDto extends Product {
    @Min(value = 1, message = "Quantity must be at least 1.")
    private Integer quantity;
}
