package com.example.tailormaster.dto;

import com.example.tailormaster.entity.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class CustomerOrderDto {
    private String customerId;

    @NotNull(message = "Order date is required.")
    @FutureOrPresent(message = "Order date cannot be in the past.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate orderDate;

    @NotNull(message = "Delivery date is required.")
    @Future(message = "Delivery date must be in the future.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate deliveryDate;

    private OrderStatus status = OrderStatus.PENDING;

    @NotNull(message = "Due payment is required.")
    @DecimalMin(value = "0.00", message = "Due payment cannot be negative.")
    private BigDecimal duePayment;

    @NotNull(message = "Total product amount is required.")
    @DecimalMin(value = "0.00", message = "Total product amount cannot be negative.")
    private BigDecimal totalProductAmount;

    @NotEmpty(message = "At least one product must be selected.")
    private List<@Valid OrderProductDto> orderProducts = new ArrayList<>();
}
