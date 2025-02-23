package com.example.tailormaster.controller;

import com.example.tailormaster.dto.CustomerOrderDto;
import com.example.tailormaster.dto.OrderProductDto;
import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.OrderProduct;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.service.customer.CustomerMeasurementService;
import com.example.tailormaster.service.customer.CustomerService;
import com.example.tailormaster.service.order.OrderService;
import com.example.tailormaster.service.product.ProductService;
import com.example.tailormaster.util.AESUtil;
import com.example.tailormaster.util.ThymeleafUtil;
import com.example.tailormaster.validation.Utility;
import com.example.tailormaster.validation.Validation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Controller
@RequestMapping("/orders")
public class OrderController {

    private final CustomerService customerService;
    private final ProductService productService;
    private final CustomerMeasurementService customerMeasurementService;
    private final OrderService orderService;

    @GetMapping
    public String listOrders(Model model) {
        List<Order> orders = orderService.getAllOrders();
        model.addAttribute("orders", orders);
        return "order/orders"; // Redirects to orders.html
    }

    // Show create order form
    @GetMapping("/create/{id}")
    public String showCreateOrderForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {

        try {
            Long customerId = Long.parseLong(AESUtil.decrypt(id));
            Customer customer = customerService.getCustomerById(customerId);
            if (customer == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Customer not found.");
                return "redirect:/customers";
            }

            CustomerOrderDto orderDto = new CustomerOrderDto();
            orderDto.setOrderDate(LocalDate.now());
            orderDto.setDeliveryDate(LocalDate.now().plusDays(1));

            List<CustomerMeasurement> measurements = customerMeasurementService.getCustomerMeasurement(customerId);
            List<Product> products = measurements.stream().map(CustomerMeasurement::getProduct).toList();

            model.addAttribute("orderDto", orderDto);
            model.addAttribute("customer", customer);
            model.addAttribute("products", products);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            return "order/create";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while creating an order");
            return "redirect:/customers"; // Handle invalid decryption cases
        }
    }

    // create order
    @PostMapping("/create")
    public String createOrder(@Valid @ModelAttribute("orderDto") CustomerOrderDto orderDto,
                              BindingResult result,
                              RedirectAttributes redirectAttributes,
                              Model model,
                              @RequestParam("encryptedCustomerId") String encryptedCustomerId) {
        try {

            // Decrypt the customer ID from the hidden field
            String decryptedId = AESUtil.decrypt(encryptedCustomerId);

            // Validate if it's a valid number
            if (!decryptedId.matches("\\d+")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid customer ID.");
                return "redirect:/customers";
            }

            // Convert decrypted ID to Long
            Long actualCustomerId = Long.parseLong(decryptedId);

            Customer customer = customerService.getCustomerById(actualCustomerId);
            if (customer == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Customer not found.");
                return "redirect:/customers";
            }

//            if (orderDto.getOrderProducts() == null || orderDto.getOrderProducts().isEmpty()) {
//                result.rejectValue("orderProducts", "error.orderProducts", "At least one product must be selected.");
//            }
//            else {
//                // Validate each product quantity
//                for (int i = 0; i < orderDto.getOrderProducts().size(); i++) {
//                    OrderProductDto product = orderDto.getOrderProducts().get(i);
//                    if (product.getQuantity() != null && product.getQuantity() < 1) {
//                        result.rejectValue("orderProducts[" + i + "].quantity",
//                                "error.orderProducts[" + i + "].quantity",
//                                "Quantity must be at least 1.");
//                    }
//                }
//            }

            if (result.hasErrors()) {
                List<CustomerMeasurement> measurements = customerMeasurementService.getCustomerMeasurement(actualCustomerId);
                List<Product> products = measurements.stream().map(CustomerMeasurement::getProduct).toList();

                model.addAttribute("orderDto", orderDto);
                model.addAttribute("customer", customer);
                model.addAttribute("products", products);
                model.addAttribute("thymeleafUtil", new ThymeleafUtil());
                result.getAllErrors().forEach(System.out::println);
                return "order/create";  // Stay on form
            }

            // save order logic will add later

            redirectAttributes.addFlashAttribute("successMessage", "Order created successfully!");
            return "redirect:/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating an order, please try again.");
            return "redirect:/orders";
        }
    }

    private String handleValidationFailure(CustomerOrderDto orderDto, Long customerId, Model model,
                                           BindingResult result, Customer customer, String field, String message) {
        // Fetch customer measurements and products
        List<CustomerMeasurement> measurements = customerMeasurementService.getCustomerMeasurement(customerId);
        List<Product> products = measurements.stream().map(CustomerMeasurement::getProduct).toList();

        // Pass necessary data back to the form
        model.addAttribute("orderDto", orderDto);
        model.addAttribute("customer", customer);
        model.addAttribute("products", products);
//        model.addAttribute("thymeleafUtil", new ThymeleafUtil());

        // Add validation error message if a specific field is provided
        if (field != null && message != null) {
            result.rejectValue(field, "error.orderDto", message);
        }

        return "order/create";  // Stay on the form
    }
}
