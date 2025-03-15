package com.example.tailormaster.dto;

import com.example.tailormaster.entity.OrderStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusUpdateDto {
    private Long orderId;
    private OrderStatus status;
    private String cabinetNo;
}
