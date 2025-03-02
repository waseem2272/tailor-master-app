package com.example.tailormaster.service.receipts;

import com.example.tailormaster.dto.ReceiptDTO;
import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.repository.UserRepository;
import com.example.tailormaster.repository.order.OrderRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class ReceiptService {
    
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public ReceiptDTO generateReceipt(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Fetch logged-in user details
        String username = getLoggedInUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create a DTO to pass data to Thymeleaf
        return new ReceiptDTO(order, user);
    }

    private String getLoggedInUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }
}
