package com.example.tailormaster.service.order;

import com.example.tailormaster.dto.OrderProductDto;
import com.example.tailormaster.dto.UpdateCustomerOrderDto;
import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.OrderProduct;
import com.example.tailormaster.entity.OrderStatus;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.repository.order.OrderRepository;
import com.example.tailormaster.repository.product.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    // Save an order
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    // Get all orders
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // Get order by ID
    public Order findById(Long id) {
        Optional<Order> order = orderRepository.findById(id);
        return order.orElse(null);
    }

    // Get orders by customer ID
    public List<Order> findByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    // Get orders by status (Enum-based)
    public List<Order> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    // Update order status
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = findById(orderId);
        if (order != null) {
            order.setStatus(newStatus);
            return orderRepository.save(order);
        }
        return null;
    }

    // Delete an order
    public void deleteById(Long id) {
        orderRepository.deleteById(id);
    }

    public int getNextOrderNumberForUser(Long userId) {
        Pageable pageable = PageRequest.of(0, 1); // Get the latest order
        List<String> lastOrderIdList = orderRepository.findLastOrderIdForUser(userId, pageable);

        String todayDatePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));

        if (lastOrderIdList.isEmpty() || lastOrderIdList.get(0) == null) {
            return 1; // Start from 001 if no previous order exists
        }

        String lastOrderId = lastOrderIdList.get(0);
        String[] parts = lastOrderId.split("-"); // Split based on '-'

        if (parts.length < 3) {
            return 1; // Fallback if format is incorrect
        }

        String lastDatePart = parts[1]; // Extract date part from last orderId

        // If the date has changed, reset the order number to 001
        if (!lastDatePart.equals(todayDatePart)) {
            return 1;
        }

        try {
            int lastNumber = Integer.parseInt(parts[2]); // Extract last number
            return lastNumber + 1;
        } catch (NumberFormatException e) {
            return 1; // Handle parsing error
        }
    }


    public Order updateOrder(Long orderId, UpdateCustomerOrderDto orderDto) {
        // 1️⃣ Fetch the existing order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        // 2️⃣ Update order details
        order.setOrderDate(orderDto.getOrderDate());
        order.setDeliveryDate(orderDto.getDeliveryDate());
        order.setTotalProductAmount(orderDto.getTotalProductAmount());
        order.setAdvancePayment(orderDto.getAdvancePayment());
        order.setDuePayment(orderDto.getDuePayment());

        // 3️⃣ Map existing products by product ID for easy lookup
        Map<Long, OrderProduct> existingProductsMap = order.getOrderProducts().stream()
                .collect(Collectors.toMap(op -> op.getProduct().getId(), op -> op));

        // 4️⃣ Update product selections
        for (OrderProductDto productDto : orderDto.getOrderProducts()) {
            Product product = productRepository.findById(productDto.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + productDto.getId()));

            if (existingProductsMap.containsKey(productDto.getId())) {
                // ✅ Update existing order product
                OrderProduct existingOrderProduct = existingProductsMap.get(productDto.getId());
                existingOrderProduct.setQuantity(productDto.getQuantity());
                existingOrderProduct.setSubtotal(productDto.getPrice().multiply(new BigDecimal(productDto.getQuantity()))); // Ensure this is calculated correctly
            } else {
                // 🔄 Add new product if it's not already in the order
                OrderProduct newOrderProduct = new OrderProduct();
                newOrderProduct.setOrder(order);
                newOrderProduct.setProduct(product);
                newOrderProduct.setQuantity(productDto.getQuantity());
                newOrderProduct.setSubtotal(productDto.getPrice().multiply(new BigDecimal(productDto.getQuantity())));

                order.getOrderProducts().add(newOrderProduct);
            }
        }

        // 5️⃣ Save the updated order
        return orderRepository.save(order);
    }


}
