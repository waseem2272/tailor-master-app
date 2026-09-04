package com.example.tailormaster.repository.orderproduct;

import com.example.tailormaster.entity.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {
//    List<OrderProduct> findByOrderIdAndProductId(long orderId, long productId);
}
