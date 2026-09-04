package com.example.tailormaster.service.orderproduct;

import com.example.tailormaster.entity.OrderProduct;
import com.example.tailormaster.repository.orderproduct.OrderProductRepository;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class OrderProductService {

    private static final Logger logger = LogManager.getLogger(OrderProductService.class);

    private final OrderProductRepository orderProductRepository;

    public Optional<OrderProduct> getOrderProductById(Long id) {
        return orderProductRepository.findById(id);
    }
}
