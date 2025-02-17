package com.example.tailormaster.dto;

import com.example.tailormaster.entity.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class CustomerOrderDto {

    @NotNull(message = "Customer ID is required.")
    private Long customerId;

    private LocalDate orderDate;

    @NotNull(message = "Delivery date is required.")
    @Future(message = "Delivery date must be in the future.")
    private LocalDate deliveryDate;

    private OrderStatus status = OrderStatus.PENDING;
    //    private BigDecimal extraCharges;
//    private String extraChargesDescription;
    @PositiveOrZero(message = "Advance payment cannot be negative.")
    private BigDecimal advancePayment;

    @NotNull(message = "Total payment is required.")
    @Positive(message = "Total payment must be greater than zero.")
    private BigDecimal totalPayment;

    @NotEmpty(message = "At least one product must be selected.")
    private List<@Valid OrderProductDto> orderProducts = new ArrayList<>();
}
