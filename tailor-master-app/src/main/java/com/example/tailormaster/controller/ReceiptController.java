package com.example.tailormaster.controller;

import com.example.tailormaster.dto.ReceiptDTO;
import com.example.tailormaster.entity.Order;
import com.example.tailormaster.service.receipts.ReceiptService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
@RequestMapping("/orders")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping("/generate-receipt/{id}")
    public String generateReceipt(@PathVariable Long id, @RequestParam String type, Model model) {
        ReceiptDTO receiptDTO = receiptService.generateReceipt(id);
        Order order = receiptDTO.getOrder();

        model.addAttribute("order", order);
        model.addAttribute("user", receiptDTO.getUser());
        model.addAttribute("receiptType", type); // "customer" or "tailor"

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
