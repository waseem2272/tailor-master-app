package com.example.tailormaster.dto;

import com.example.tailormaster.enums.OrderProductStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderProductStatusUpdateDto {
    private Long orderProductId;
    private OrderProductStatus status;
}