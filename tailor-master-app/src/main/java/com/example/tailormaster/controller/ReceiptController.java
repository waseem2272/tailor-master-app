package com.example.tailormaster.controller;

import com.example.tailormaster.dto.ReceiptDTO;
import com.example.tailormaster.entity.Order;
import com.example.tailormaster.service.order.OrderService;
import com.example.tailormaster.service.receipts.ReceiptService;
import com.example.tailormaster.util.ThymeleafUtil;
import com.example.tailormaster.validation.Validation;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@AllArgsConstructor
@Controller
@RequestMapping("/orders")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final Validation validation;
    private final OrderService orderService;

    @GetMapping("/generate-receipt/{id}")
    public String generateReceipt(@PathVariable String id, @RequestParam String type, Model model,
                                  RedirectAttributes redirectAttributes) {
        // Decrypt and validate customer ID
        Long orderId = validation.decryptAndValidateId(id);
        if (orderId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid order ID.");
            return "redirect:/orders";
        }

        Order order = orderService.findById(orderId);
        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Order not found.");
            return "redirect:/orders";
        }

        ReceiptDTO receiptDTO = receiptService.generateReceipt(order);
        Order order1 = receiptDTO.getOrder();

        model.addAttribute("order", order1);
        model.addAttribute("user", receiptDTO.getUser());
        model.addAttribute("receiptType", type); // "customer" or "tailor"
        model.addAttribute("thymeleafUtil", new ThymeleafUtil());

        // Calculate due payment if necessary
        BigDecimal totalAmount = order.getTotalProductAmount();
        BigDecimal advancePayment = order.getAdvancePayment();

        // Ensure values are not null before subtraction
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
        if (advancePayment == null) {
            advancePayment = BigDecimal.ZERO;
        }

        BigDecimal dueAmount = totalAmount.subtract(advancePayment);
        order.setDuePayment(dueAmount);

        return "order/receipt"; // Show the receipt page
    }
}
