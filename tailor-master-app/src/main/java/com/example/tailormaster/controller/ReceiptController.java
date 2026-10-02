package com.example.tailormaster.controller;

import com.example.tailormaster.dto.ReceiptDTO;
import com.example.tailormaster.entity.*;
import com.example.tailormaster.service.customer.CustomerMeasurementService;
import com.example.tailormaster.service.customer.CustomerService;
import com.example.tailormaster.service.order.OrderService;
import com.example.tailormaster.service.orderproduct.OrderProductService;
import com.example.tailormaster.service.product.ProductService;
import com.example.tailormaster.service.receipts.ReceiptService;
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
    private final CustomerService customerService;
    private final ProductService productService;

    @GetMapping("/generate-order-receipt/{id}")
    public String generateOrderReceipt(@PathVariable String id,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {

        logger.info("Attempting to generate order receipt for order ID: {}", id);

        try {

            Long orderId = validation.decryptAndValidateId(id);

            if (orderId == null) {
                logger.warn("Invalid order ID provided for receipt generation: {}", id);
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Invalid order ID."
                );
                return "redirect:/orders";
            }

            Order order = orderService.findById(orderId);

            if (order == null) {
                logger.warn("Order not found with ID {}", orderId);
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Order not found."
                );
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

            model.addAttribute(
                    "thymeleafUtil",
                    new ThymeleafUtil()
            );

            /*
             * Calculate due amount only for display.
             * Do NOT modify order entity.
             */
            BigDecimal totalAmount =
                    Optional.ofNullable(order.getTotalProductAmount())
                            .orElse(BigDecimal.ZERO);

            BigDecimal advancePayment =
                    Optional.ofNullable(order.getAdvancePayment())
                            .orElse(BigDecimal.ZERO);

            BigDecimal dueAmount =
                    totalAmount.subtract(advancePayment);

            model.addAttribute("displayDueAmount", dueAmount);

            logger.debug(
                    "Receipt DTO generated successfully for order ID: {}",
                    orderId
            );

            return "order/receipt";

        } catch (Exception e) {

            logger.error(
                    "An error occurred while generating receipt for order ID {}: {}", id, e.getMessage(), e
            );

            redirectAttributes.addFlashAttribute(
                    "errorMessage", "An error occurred while generating the receipt."
            );

            return "redirect:/orders";
        }
    }

    @GetMapping("/generate-measurement/{id}")
    public String generateMeasurement(@PathVariable String id, Model model,
                                  RedirectAttributes redirectAttributes) {
        logger.info("Attempting to generate measurement for order product ID: {}", id);
        try {
            // Decrypt and validate customer ID
            Long orderProductId = validation.decryptAndValidateId(id);
            if (orderProductId == null) {
                logger.warn("Invalid orderProductId provided for measurement generation: {}", id);
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid orderProductId.");
                return "redirect:/orders";
            }
            logger.debug("Decrypted orderProductId for measurement generation: {}", orderProductId);

            OrderProduct orderProduct = orderProductService.getOrderProductById(orderProductId).orElse(null);
            if (orderProduct == null) {
                logger.warn("Order Product not found with ID {} for measurement generation.", orderProductId);
                redirectAttributes.addFlashAttribute("errorMessage", "Order Product not found.");
                return "redirect:/orders";
            }
            logger.debug("Fetched order product for measurement generation: {}", orderProductId);

            Order order = orderService.findById(orderProduct.getOrder().getId());
            if (order == null) {
                logger.warn("Order not found with ID {} for measurement generation.", orderProductId);
                redirectAttributes.addFlashAttribute("errorMessage", "Order not found.");
                return "redirect:/orders";
            }
            logger.debug("Fetched order for measurement generation: {}", order);

            // get customer
            Customer customer = order.getCustomer();
            if (customer == null) {
                logger.warn("Customer not found with ID {} for measurement generation.", orderProductId);
                redirectAttributes.addFlashAttribute("errorMessage", "Customer not found.");
                return "redirect:/orders";
            }
            logger.info("Fetched customer for measurement generation: {}", customer);
            // get measurement

            List<CustomerMeasurement> measurement = customerMeasurementService.getMeasurement(customer.getId(), orderProduct.getProduct().getId());
            if (measurement == null || measurement.isEmpty()) {
                logger.warn("Measurement not found with customer ID: {}", customer.getId());
                redirectAttributes.addFlashAttribute("errorMessage", "Measurement not found.");
                return "redirect:/orders";
            }
            logger.debug("Fetched customer measurement: {}", measurement);

            model.addAttribute("order", order);
            model.addAttribute("orderProduct", orderProduct);
            model.addAttribute("quantity", orderProduct.getQuantity());
            model.addAttribute("measurement", measurement);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());

            return "order/customer-measurement"; // Show the measurement page
        } catch (Exception e) {
            logger.error("An error occurred while generating receipt for order ID {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while generating the receipt.");
            return "redirect:/orders";
        }
    }

//    @GetMapping("/generate-measurement/{customerId}/{productId}")
//    public String generateMeasurementForCustomerDetails(@PathVariable String customerId, @PathVariable String productId, Model model,
//                                      RedirectAttributes redirectAttributes) {
//        logger.info("Attempting to generate measurement for customer ID: {}, product ID: {}", customerId, productId);
//        try {
//            // Decrypt and validate customer ID
//            Long customerIdd = validation.decryptAndValidateId(customerId);
//            Long productIdd = validation.decryptAndValidateId(productId);
//            if (customerIdd == null || productIdd == null) {
//                logger.warn("Invalid customerI:d {} productId: {} provided for measurement generation.", customerId, productId);
//                redirectAttributes.addFlashAttribute("errorMessage", "Invalid customerId.");
//                return "redirect:/customers";
//            }
//            logger.debug("Decrypted customerId: {} productId: {} for measurement generation", customerIdd, productIdd);
//
//            // get customer
//            Customer customer = customerService.getCustomerById(customerIdd);
//            if (customer == null) {
//                logger.warn("Customer not found with ID {} for measurement generation.", customerIdd);
//                redirectAttributes.addFlashAttribute("errorMessage", "Customer not found.");
//                return "redirect:/customers";
//            }
//            logger.info("Fetched customer for measurement generation: {}", customer);
//            // get product
//            Product product = productService.getProductById(productIdd);
//            if (product == null) {
//                logger.warn("Product not found with ID {} for measurement generation.", productIdd);
//                redirectAttributes.addFlashAttribute("errorMessage", "Product not found.");
//                return "redirect:/customers";
//            }
//            logger.info("Fetched customer for measurement generation: {}", customer);
//            // get measurement
//
//            List<CustomerMeasurement> measurement = customerMeasurementService.getMeasurement(customerIdd, productIdd);
//            if (measurement == null || measurement.isEmpty()) {
//                logger.warn("Measurement not found with customer ID: {}", customer.getId());
//                redirectAttributes.addFlashAttribute("errorMessage", "Measurement not found.");
//                return "redirect:/customers";
//            }
//            logger.debug("Fetched customer measurement: {}", measurement);
//
//            model.addAttribute("customer", customer);
//            model.addAttribute("product", product);
//            model.addAttribute("measurement", measurement);
//            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
//
//            return "order/customer-measurement"; // Show the measurement page
//        } catch (Exception e) {
//            logger.error("An error occurred while generating measurement for customer ID {}: product ID {}: {}", customerId, productId, e.getMessage(), e);
//            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while generating the Measurement.");
//            return "redirect:/customers";
//        }
//    }

//    @GetMapping("/receipts/generate-measurement/{customerId}/{productId}")
//    public String generateMeasurement(
//            @PathVariable String customerId,
//            @PathVariable String productId,
//            Model model, RedirectAttributes redirectAttributes) {
//
//        Long customerDbId = validation.decryptAndValidateId(customerId);
//        Long productDbId = validation.decryptAndValidateId(productId);
//
//        Customer customer = customerService.getCustomerById(customerDbId);
//        Product product = productService.getProductById(productDbId);
//
//        List<CustomerMeasurement> measurement =
//                customerMeasurementService.getMeasurement(
//                        customerDbId,
//                        productDbId
//                );
//
//        if (measurement == null || measurement.isEmpty()) {
//                logger.warn("Measurement not found with customer ID: {}", customer.getId());
//                redirectAttributes.addFlashAttribute("errorMessage", "Measurement not found.");
//                return "redirect:/customers";
//            }
//            logger.debug("Fetched customer measurement: {}", measurement);
//
//        model.addAttribute("customer", customer);
//        model.addAttribute("product", product);
//        model.addAttribute("measurement", measurement);
//
//        return "receipts/measurement";
//    }
}
