package com.example.tailormaster.dto;

import com.example.tailormaster.entity.OrderStatus;
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
    private Long customerId;
    private LocalDate orderDate;
    private LocalDate deliveryDate;
    private OrderStatus status = OrderStatus.PENDING;
//    private BigDecimal extraCharges;
//    private String extraChargesDescription;
    private BigDecimal advancePayment;
    private BigDecimal totalPayment;
    private List<OrderProductDto> orderProducts = new ArrayList<>();}
