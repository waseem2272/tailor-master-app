package com.example.tailormaster.dto;

import com.example.tailormaster.entity.Customer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UpdateCustomerOrderDto {

    private Customer customer;
    
//    @NotNull(message = "Order ID is required.")
    private Long id;

    @NotNull(message = "Order date is required.")
    private LocalDate orderDate;

    @NotNull(message = "Delivery date is required.")
    private LocalDate deliveryDate;

    @DecimalMin(value = "0.0", inclusive = true, message = "Advance payment cannot be negative.")
    private BigDecimal advancePayment;

    @NotNull(message = "Due payment is required.")
    @DecimalMin(value = "0.00", message = "Due payment cannot be negative.")
    private BigDecimal duePayment;

    @NotNull(message = "Total product amount is required.")
    @DecimalMin(value = "0.00", message = "Total product amount cannot be negative.")
    private BigDecimal totalProductAmount;

    private BigDecimal paidAmount;

    @NotEmpty(message = "At least one product must be selected.")
    private List<@Valid OrderProductDto> orderProducts = new ArrayList<>();

}
