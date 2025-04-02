package com.example.tailormaster.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class PendingPaymentRequest {
    private String orderId;
    private BigDecimal paymentAmount;

    // Getters and Setters
}
