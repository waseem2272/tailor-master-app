package com.example.tailormaster.dto;

import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ReceiptDTO {
    private Order order;
    private User user;
}
