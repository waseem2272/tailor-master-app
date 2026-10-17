package com.example.tailormaster.controller;

import com.example.tailormaster.dto.ReceiptDTO;
import com.example.tailormaster.entity.*;
import com.example.tailormaster.service.customer.CustomerMeasurementService;
import com.example.tailormaster.service.customer.CustomerService;
import com.example.tailormaster.service.order.OrderService;
import com.example.tailormaster.service.orderproduct.OrderProductService;
import com.example.tailormaster.service.product.ProductService;
import com.example.tailormaster.service.receipts.ReceiptService;
import com.example.tailormaster.util.MessageUtil;
import com.example.tailormaster.util.ThymeleafUtil;
import com.example.tailormaster.validation.Utility;
import com.example.tailormaster.validation.Validation;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Controller
@RequestMapping("/receipts")
public class ReceiptController {
    private static final Logger logger = LogManager.getLogger(ReceiptController.class);
    private final ReceiptService receiptService;
    private final Validation validation;
    private final OrderService orderService;
    private final CustomerMeasurementService customerMeasurementService;
    private final OrderProductService orderProductService;
    private final MessageUtil messageUtil;

    @GetMapping("/generate-order-receipt/{id}")
    public String generateOrderReceipt(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Attempting to generate order receipt for order ID: {}", id);
        try {
            Long orderId = validation.decryptAndValidateId(id);
            if (orderId == null) {
                logger.warn("Invalid order ID provided for receipt generation: {}", id);
                redirectAttributes.addFlashAttribute("errorMessage", messageUtil.get("receipt.error.invalidOrderId"));
                return "redirect:/orders";
            }
            Order order = orderService.findById(orderId);
            if (order == null) {
                logger.warn("Order not found with ID {}", orderId);
                redirectAttributes.addFlashAttribute("errorMessage", messageUtil.get("receipt.error.orderNotFound"));
                return "redirect:/orders";
            }
            logger.debug("Fetched order for receipt generation: {}", orderId);
            ReceiptDTO receiptDTO = receiptService.generateOrderReceipt(order);
            User user = receiptDTO.getUser();
            byte[] logoData = user.getLogo();
            String logoBase64 = null;
            String logoMimeType = null;
            if (logoData != null && logoData.length > 0) {
                logoBase64 = Base64.getEncoder().encodeToString(logoData);
                logoMimeType = Utility.detectImageMimeType(logoData);
            }
            model.addAttribute("order", receiptDTO.getOrder());
            model.addAttribute("user", user);
            model.addAttribute("logoBase64", logoBase64);
            model.addAttribute("logoMimeType", logoMimeType);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            BigDecimal totalAmount = Optional.ofNullable(order.getTotalProductAmount()).orElse(BigDecimal.ZERO);
            BigDecimal advancePayment = Optional.ofNullable(order.getAdvancePayment()).orElse(BigDecimal.ZERO);
            BigDecimal dueAmount = totalAmount.subtract(advancePayment);
            model.addAttribute("displayDueAmount", dueAmount);
            logger.debug("Receipt DTO generated successfully for order ID: {}", orderId);
            return "order/receipt";
        } catch (Exception e) {
            logger.error("An error occurred while generating receipt for order ID {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", messageUtil.get("receipt.error.generating"));
            return "redirect:/orders";
        }
    }

    @GetMapping("/generate-measurement/{id}")
    public String generateMeasurement(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Attempting to generate measurement for order product ID: {}", id);
        try {
            Long orderProductId = validation.decryptAndValidateId(id);
            if (orderProductId == null) {
                logger.warn("Invalid orderProductId provided for measurement generation: {}", id);
                redirectAttributes.addFlashAttribute("errorMessage", messageUtil.get("receipt.error.invalidOrderProductId"));
                return "redirect:/orders";
            }
            logger.debug("Decrypted orderProductId for measurement generation: {}", orderProductId);
            OrderProduct orderProduct = orderProductService.getOrderProductById(orderProductId).orElse(null);
            if (orderProduct == null) {
                logger.warn("Order Product not found with ID {} for measurement generation.", orderProductId);
                redirectAttributes.addFlashAttribute("errorMessage", messageUtil.get("receipt.error.orderProductNotFound"));
                return "redirect:/orders";
            }
            logger.debug("Fetched order product for measurement generation: {}", orderProductId);
            Order order = orderService.findById(orderProduct.getOrder().getId());
            if (order == null) {
                logger.warn("Order not found with ID {} for measurement generation.", orderProductId);
                redirectAttributes.addFlashAttribute("errorMessage", messageUtil.get("receipt.error.orderNotFound"));
                return "redirect:/orders";
            }
            logger.debug("Fetched order for measurement generation: {}", order);
            Customer customer = order.getCustomer();
            if (customer == null) {
                logger.warn("Customer not found with ID {} for measurement generation.", orderProductId);
                redirectAttributes.addFlashAttribute("errorMessage", messageUtil.get("receipt.error.customerNotFound"));
                return "redirect:/orders";
            }
            logger.info("Fetched customer for measurement generation: {}", customer);
            CustomerProductMeasurement measurement = customerMeasurementService.getCustomerProductMeasurement(
                    customer.getId(), orderProduct.getProduct().getId());
            List<CustomerMeasurement> customerMeasurements =
                    customerMeasurementService.getCustomerMeasurementsByParentId(measurement.getId());
            if (customerMeasurements == null || customerMeasurements.isEmpty()) {
                logger.warn("Measurement not found with customer ID: {}", customer.getId());
                redirectAttributes.addFlashAttribute("errorMessage", messageUtil.get("receipt.error.measurementNotFound"));
                return "redirect:/orders";
            }
            logger.debug("Fetched customer product measurement: {}", measurement);
            logger.debug("Fetched customer measurements: {}", customerMeasurements);
            model.addAttribute("order", order);
            model.addAttribute("orderProduct", orderProduct);
            model.addAttribute("quantity", orderProduct.getQuantity());
            model.addAttribute("measurement", measurement);
            model.addAttribute("customerMeasurements", customerMeasurements);
            model.addAttribute("backToOrder", true);
            model.addAttribute("backOrderId", new ThymeleafUtil().encryptId(order.getId()));
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            return "customer/customer-measurement";
        } catch (Exception e) {
            logger.error("An error occurred while generating receipt for order ID {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", messageUtil.get("customer.details.loadMeasurementError"));
            return "redirect:/orders";
        }
    }
}
