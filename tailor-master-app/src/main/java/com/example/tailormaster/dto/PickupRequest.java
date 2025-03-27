package com.example.tailormaster.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class PickupRequest {
    private Long orderId;
    private BigDecimal amountReceived;

    // Getters and Setters
}
