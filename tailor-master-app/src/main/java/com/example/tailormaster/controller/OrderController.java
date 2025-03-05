package com.example.tailormaster.controller;

import com.example.tailormaster.dto.CustomerOrderDto;
import com.example.tailormaster.dto.OrderProductDto;
import com.example.tailormaster.entity.*;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.service.UserService;
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
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@Controller
@RequestMapping("/orders")
public class OrderController {

    private final CustomerService customerService;
    private final ProductService productService;
    private final CustomerMeasurementService customerMeasurementService;
    private final OrderService orderService;
    private final UserService userService;

    @GetMapping
    public String listOrders(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        model.addAttribute("thymeleafUtil", new ThymeleafUtil());
        return "order/orders"; // Redirects to orders.html
    }

    // Show create order form
    @GetMapping("/create/{id}")
    public String showCreateOrderForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {

        try {
            // Decrypt and validate customer ID
            Long customerId = validateAndFetchCustomer(id, redirectAttributes);
            if (customerId == null) {
                return "redirect:/customers";
            }

            // Fetch customer details (since ID is valid)
            Customer customer = customerService.getCustomerById(customerId);

            CustomerOrderDto orderDto = new CustomerOrderDto();
            orderDto.setOrderDate(LocalDate.now());
            orderDto.setDeliveryDate(LocalDate.now().plusDays(1));

            return populateModel(model, customerId, orderDto, customer);

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while creating an order");
            return "redirect:/customers"; // Handle invalid decryption cases
        }
    }

    @PostMapping("/create")
    public String createOrder(@Valid @ModelAttribute("orderDto") CustomerOrderDto orderDto,
                              BindingResult result,
                              RedirectAttributes redirectAttributes,
                              Model model,
                              @RequestParam("encryptedCustomerId") String encryptedCustomerId,
                              Principal principal) {
        try {
            // Decrypt and validate customer ID
            Long customerId = validateAndFetchCustomer(encryptedCustomerId, redirectAttributes);
            if (customerId == null) {
                return "redirect:/customers";
            }

            // Fetch customer details (since ID is valid)
            Customer customer = customerService.getCustomerById(customerId);

            // Perform validation checks
            validateOrder(orderDto, result);
            if (result.hasErrors()) {
                return populateModel(model, customerId, orderDto, customer);
            }

            Optional<User> user = userService.findByUsername(principal.getName());
            if (user.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
                return "redirect:/orders";
            }

            // Generate Order ID
            String orderId = generateOrderId(user.get());

            // Save Order Logic
            Order order = buildOrder(orderDto, customerId);
            order.setUser(user.get());
            order.setOrderId(orderId);

            Order savedOrder = orderService.save(order);

            redirectAttributes.addFlashAttribute("successMessage", "Order created successfully!");
            redirectAttributes.addFlashAttribute("orderId", savedOrder.getId());
            return "redirect:/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating an order, please try again.");
            return "redirect:/orders";
        }
    }

    private Long validateAndFetchCustomer(String encryptedCustomerId, RedirectAttributes redirectAttributes) {
        Long customerId = decryptAndValidateId(encryptedCustomerId);
        if (customerId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid customer ID.");
            return null;
        }

        Customer customer = customerService.getCustomerById(customerId);
        if (customer == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Customer not found.");
            return null;
        }

        return customerId;
    }


    private Long decryptAndValidateId(String encryptedId) {
        try {
            String decryptedId = AESUtil.decrypt(encryptedId);
            if (!decryptedId.matches("\\d+")) {
                return null;
            }
            return Long.parseLong(decryptedId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private void validateOrder(CustomerOrderDto orderDto, BindingResult result) {
        if (orderDto.getOrderProducts() == null || orderDto.getOrderProducts().isEmpty()) {
            result.rejectValue("orderProducts", "error.orderProducts", "At least one product must be selected.");
            return;
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (int i = 0; i < orderDto.getOrderProducts().size(); i++) {
            OrderProductDto product = orderDto.getOrderProducts().get(i);

            // Validate Quantity
            if (product.getQuantity() == null || product.getQuantity() < 1) {
                result.rejectValue("orderProducts[" + i + "].quantity",
                        "error.orderProducts[" + i + "].quantity",
                        "Quantity must be at least 1.");
            }

            // Validate Product Price
            if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                result.rejectValue("orderProducts[" + i + "].price",
                        "error.orderProducts[" + i + "].price",
                        "Price must be greater than zero.");
            }

            // Calculate Total Amount
            if (product.getPrice() != null && product.getQuantity() != null) {
                totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(product.getQuantity())));
            }
        }

        // Validate Total Amount
        if (orderDto.getTotalProductAmount().compareTo(totalAmount) != 0) {
            result.rejectValue("totalProductAmount", "error.totalProductAmount", "Total amount is incorrect.");
        }

        // Validate Advance Payment
        if (orderDto.getAdvancePayment().compareTo(totalAmount) > 0) {
            result.rejectValue("advancePayment", "error.advancePayment", "Advance payment cannot exceed total amount.");
        }

        // Validate Due Payment
        BigDecimal expectedDuePayment = totalAmount.subtract(orderDto.getAdvancePayment());
        if (orderDto.getDuePayment().compareTo(expectedDuePayment) != 0) {
            result.rejectValue("duePayment", "error.duePayment", "Due payment is incorrect.");
        }
    }


    private String populateModel(Model model, Long customerId, CustomerOrderDto orderDto, Customer customer) {
        List<CustomerMeasurement> measurements = customerMeasurementService.getCustomerMeasurement(customerId);
        List<Product> products = measurements.stream().map(CustomerMeasurement::getProduct).toList();

        model.addAttribute("orderDto", orderDto);
        model.addAttribute("customer", customer);
        model.addAttribute("products", products);
        model.addAttribute("thymeleafUtil", new ThymeleafUtil());
        return "order/create";
    }

    private Order buildOrder(CustomerOrderDto orderDto, Long actualCustomerId) {
        Order order = new Order();
        order.setOrderDate(orderDto.getOrderDate());
        order.setDeliveryDate(orderDto.getDeliveryDate());
        order.setStatus(orderDto.getStatus());
        order.setAdvancePayment(orderDto.getAdvancePayment());
        order.setDuePayment(orderDto.getDuePayment());

        // Fetch customer and associate with order
        Customer customer = customerService.getCustomerById(actualCustomerId);
        order.setCustomer(customer);

        // Convert OrderProductDto list to OrderProduct entities
        List<OrderProduct> orderProducts = orderDto.getOrderProducts().stream().map(opDto -> {
            OrderProduct orderProduct = new OrderProduct();
            Product product = productService.getProductById(opDto.getId());
            orderProduct.setProduct(product);
            orderProduct.setQuantity(opDto.getQuantity());
            orderProduct.setSubtotal(product.getPrice().multiply(new BigDecimal(opDto.getQuantity())));
            orderProduct.setOrder(order);
            return orderProduct;
        }).collect(Collectors.toList());

        order.setTotalProductAmount(orderDto.getTotalProductAmount());

        order.setOrderProducts(orderProducts);
        return order;
    }

    @GetMapping("/details/{id}")
    public String showOrderDetails(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {

        try {
            // Decrypt and validate customer ID
            Long orderId = decryptAndValidateId(id);
            if (orderId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid order ID.");
                return "redirect:/orders";
            }

            Order order = orderService.findById(orderId);
            model.addAttribute("order", order);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "order/order-details";
    }

    private String generateOrderId(User user) {
        int orderNumber = orderService.getNextOrderNumberForUser(user.getId());
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        return String.format("%s-%s-%03d", user.getShortCode(), datePart, orderNumber);
    }

}
