package com.example.tailormaster.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class OrderProductDto {
    private Long productId;
    private int quantity;
}
