package com.example.tailormaster.controller;

import com.example.tailormaster.dto.*;
import com.example.tailormaster.entity.*;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.enums.OrderStatus;
import com.example.tailormaster.service.UserService;
import com.example.tailormaster.service.customer.CustomerMeasurementService;
import com.example.tailormaster.service.customer.CustomerService;
import com.example.tailormaster.service.order.OrderService;
import com.example.tailormaster.service.product.ProductService;
import com.example.tailormaster.util.ThymeleafUtil;
import com.example.tailormaster.validation.Validation;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@AllArgsConstructor
@Controller
@RequestMapping("/orders")
@SessionAttributes("orderDto")
public class OrderController {

    private static final Logger logger = LogManager.getLogger(OrderController.class);

    private final CustomerService customerService;
    private final ProductService productService;
    private final CustomerMeasurementService customerMeasurementService;
    private final OrderService orderService;
    private final UserService userService;
    private final Validation validation;

    @GetMapping
    public String listOrders(Model model) {
        logger.info("User accessed the orders list page.");
        model.addAttribute("thymeleafUtil", new ThymeleafUtil());
        model.addAttribute("orderStatusList", OrderStatus.values());
        return "order/orders"; // Redirects to orders.html
    }

    @GetMapping("/datatable")
    public ResponseEntity<Map<String, Object>> getOrdersDatatable(
            @RequestParam("draw") int draw,
            @RequestParam("start") int start,
            @RequestParam("length") int length,
            @RequestParam(value = "search[value]", required = false) String searchValue,
            @RequestParam(value = "order[0][column]", required = false) Integer columnIndex,
            @RequestParam(value = "order[0][dir]", required = false) String sortDirection,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "orderStatus", required = false) OrderStatus orderStatus,
            @RequestParam(value = "paymentStatus", required = false) String paymentStatus) {

        try {
            logger.info("Fetching paginated orders for datatable - draw: {}, start: {}, length: {}, search: {}, column: {}, direction: {}, startDate: {}, endDate: {}",
                    draw, start, length, searchValue, columnIndex, sortDirection, startDate, endDate);
            Map<String, Object> response = orderService.getPaginatedOrders(draw, start, length, searchValue, columnIndex, sortDirection, startDate, endDate, orderStatus, paymentStatus);
            logger.debug("Successfully fetched {} orders for datatable.", ((List<?>) response.get("data")).size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters for orders datatable: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse("Invalid request parameters: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("An unexpected error occurred while fetching orders for datatable: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("An unexpected error occurred. Please try again."));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", true);
        errorResponse.put("message", message);
        return errorResponse;
    }

    // Show create order form
    @GetMapping("/create/{id}")
    public String showCreateOrderForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Displaying create order form for customer ID: {}", id);
        try {
            // Decrypt and validate customer ID
            Long customerId = validation.validateAndFetchCustomer(id, redirectAttributes);
            if (customerId == null) {
                logger.warn("Invalid or missing customer ID for create order form.");
                return "redirect:/customers";
            }

            // Fetch customer details (since ID is valid)
            Customer customer = customerService.getCustomerById(customerId);
            logger.debug("Fetched customer details for ID {}: {}", customerId, customer);

            CustomerOrderDto orderDto = new CustomerOrderDto();
            orderDto.setOrderDate(LocalDate.now());
            orderDto.setDeliveryDate(LocalDate.now().plusDays(1));

            return populateModel(model, customerId, orderDto, customer);

        } catch (Exception e) {
            logger.error("An error occurred while preparing the create order form for customer ID {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while creating an order, please try again.");
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
        logger.info("Attempting to create a new order for customer ID: {}", encryptedCustomerId);
        try {
            // Decrypt and validate customer ID
            Long customerId = validation.validateAndFetchCustomer(encryptedCustomerId, redirectAttributes);
            if (customerId == null) {
                logger.warn("Invalid or missing customer ID during order creation.");
                return "redirect:/customers";
            }

            // Fetch customer details (since ID is valid)
            Customer customer = customerService.getCustomerById(customerId);
            logger.debug("Fetched customer details for order creation (ID {}): {}", customerId, customer);
            // Perform validation checks
            validateOrder(orderDto, result);
            if (result.hasErrors()) {
                logger.warn("Validation errors occurred during order creation for customer ID {}: {}", customerId, result.getAllErrors());
                model.addAttribute("org.springframework.validation.BindingResult.orderDto", result);
                return populateModel(model, customerId, orderDto, customer);
            }

            Optional<User> user = userService.findByUsername(principal.getName());
            if (user.isEmpty()) {
                logger.error("User not found with username: {}", principal.getName());
                redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
                return "redirect:/customers";
            }
            logger.debug("User found: {}", user.get().getUsername());
            // Generate Order ID
            String orderId = generateOrderId(user.get());
            logger.debug("Generated order ID: {}", orderId);
            // Save Order Logic
            Order order = buildOrder(orderDto, customerId);
            order.setUser(user.get());
            order.setOrderId(orderId);

            Order savedOrder = orderService.save(order);
            logger.info("Order created successfully: {}", savedOrder);

            redirectAttributes.addFlashAttribute("successMessage", "Order created successfully!");
            redirectAttributes.addFlashAttribute("orderId", savedOrder.getId());
            return "redirect:/orders";
        } catch (Exception e) {
            logger.error("An error occurred while creating an order for customer ID {}: {}", encryptedCustomerId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while creating an order, please try again.");
            return "redirect:/customers";
        }
    }

    private void validateOrder(CustomerOrderDto orderDto, BindingResult result) {

        if (result.hasErrors()) {
            return;
        }

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

    private void validateOrder(UpdateCustomerOrderDto orderDto, BindingResult result) {

        if (result.hasErrors()) {
            return;
        }

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
        orderDto.setCustomer(customer);
        model.addAttribute("orderDto", orderDto);
//        model.addAttribute("customer", customer);
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
        order.setPaidAmount(orderDto.getAdvancePayment());

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
            Long orderId = validation.decryptAndValidateId(id);
            if (orderId == null) {
                logger.warn("Invalid order ID provided for details.");
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid order ID.");
                return "redirect:/orders";
            }

            Order order = orderService.findById(orderId);
            if (order == null) {
                logger.warn("Order not found with ID: {}", orderId);
                redirectAttributes.addFlashAttribute("errorMessage", "Order not found.");
                return "redirect:/orders";
            }
            logger.info("Displaying order details: {}", order);
            model.addAttribute("order", order);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            return "order/order-details";
        } catch (Exception e) {
            logger.error("An error occurred while fetching details for order ID {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while fetching order details.");
            return "redirect:/orders";
        }
    }

    private String generateOrderId(User user) {
        try {
            int orderNumber = orderService.getNextOrderNumberForUser(user.getId());
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
            String orderId = String.format("%s-%s-%03d", user.getShortCode(), datePart, orderNumber);
            logger.debug("Generated order ID: {} for user: {}", orderId, user.getUsername());
            return orderId;
        } catch (Exception e) {
            logger.error("Error generating order ID for user {}: {}", user.getUsername(), e.getMessage(), e);
            throw new RuntimeException("Error generating order ID.", e);
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditOrderForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Displaying edit order form for ID: {}", id);
        try {
            // Decrypt and validate order ID
            Long orderId = validation.validateAndFetchOrder(id, redirectAttributes);
            if (orderId == null) {
                logger.warn("Invalid order ID provided for editing.");
                return "redirect:/orders";
            }

            Order order = orderService.findById(orderId);
            if (order == null) {
                logger.warn("Order not found with ID {} for editing.", orderId);
                redirectAttributes.addFlashAttribute("errorMessage", "Order not found.");
                return "redirect:/orders";
            }
            List<Map<String, Object>> orderProducts = orderService.getOrderProducts(order);
            logger.info("Fetched order details for edit: {}: and order products: {}", order, orderProducts);
            UpdateCustomerOrderDto orderUpdateDto = populateOrderUpdateDto(order);
            model.addAttribute("orderDto", orderUpdateDto);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            return "order/update";

        } catch (Exception e) {
            logger.error("An error occurred while preparing the edit order form for ID {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while preparing the edit order: " + e.getMessage());
            return "redirect:/orders"; // Keep only one return statement
        }
    }

    private UpdateCustomerOrderDto populateOrderUpdateDto(Order order) {
        logger.debug("Populating UpdateCustomerOrderDto for order ID: {}", order.getId());
        UpdateCustomerOrderDto orderUpdateDto = new UpdateCustomerOrderDto();

        // Set order details
        orderUpdateDto.setId(order.getId());
        orderUpdateDto.setCustomer(order.getCustomer());
        orderUpdateDto.setOrderDate(order.getOrderDate());
        orderUpdateDto.setDeliveryDate(order.getDeliveryDate());
//        orderUpdateDto.setStatus(order.getStatus());
        orderUpdateDto.setAdvancePayment(order.getAdvancePayment());
        orderUpdateDto.setDuePayment(order.getDuePayment());
        orderUpdateDto.setTotalProductAmount(order.getTotalProductAmount());
        orderUpdateDto.setPaidAmount(order.getAdvancePayment());

        // Populate order products
        List<OrderProductDto> orderProductDtos = order.getOrderProducts().stream().map(orderProduct -> {
            OrderProductDto orderProductDto = new OrderProductDto();
            orderProductDto.setId(orderProduct.getProduct().getId());
            orderProductDto.setName(orderProduct.getProduct().getName());
            orderProductDto.setQuantity(orderProduct.getQuantity());
            orderProductDto.setPrice(orderProduct.getProduct().getPrice());
            return orderProductDto;
        }).collect(Collectors.toList());
        orderUpdateDto.setOrderProducts(orderProductDtos);
        logger.debug("Successfully populated UpdateCustomerOrderDto with {} product(s).", orderProductDtos.size());
        return orderUpdateDto;
    }

    @PostMapping("/update")
    public String updateOrder(
            @ModelAttribute("orderDto") @Valid UpdateCustomerOrderDto orderUpdateDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            @RequestParam("encryptedOrderId") String encryptedOrderId) {
        logger.info("Attempting to update order with encrypted order ID: {}", encryptedOrderId);
        try {
            // Decrypt and validate order ID
            Long orderId = validation.validateAndFetchOrder(encryptedOrderId, redirectAttributes);
            if (orderId == null) {
                logger.warn("Invalid order ID provided for update: {}", encryptedOrderId);
                return "redirect:/orders";
            }
            logger.debug("Decrypted order ID for update: {}", orderId);
            // Fetch order details (since ID is valid)
            Order order = orderService.findById(orderId);
            if (order == null) {
                logger.warn("Order not found with ID {} for update.", orderId);
                redirectAttributes.addFlashAttribute("errorMessage", "Order not found.");
                return "redirect:/orders";
            }
//            logger.info("Fetched order for update (ID {}): {}", orderId, order);
            // Perform validation checks
            validateOrder(orderUpdateDto, bindingResult);
            if (bindingResult.hasErrors()) {
                logger.warn("Validation errors occurred during order update for ID {}: {}", orderId, bindingResult.getAllErrors());
                model.addAttribute("org.springframework.validation.BindingResult.orderDto", bindingResult);
                UpdateCustomerOrderDto orderUpdateDto1 = populateOrderUpdateDto(order);
                model.addAttribute("orderDto", orderUpdateDto1);
                model.addAttribute("thymeleafUtil", new ThymeleafUtil());
                return "order/update";
            }

            Order updatedOrder = orderService.updateOrder(orderId, orderUpdateDto);
            logger.info("Order updated successfully: {}", updatedOrder);
            redirectAttributes.addFlashAttribute("successMessage", "Order updated successfully!");
            redirectAttributes.addFlashAttribute("orderId", updatedOrder.getId());
            return "redirect:/orders";
        } catch (Exception e) {
            logger.error("An error occurred while updating order with encrypted ID {}: {}", encryptedOrderId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating an order, please try again.");
            return "redirect:/orders";
        }
    }

    @PostMapping("/update-order-status")
    @ResponseBody
    public ResponseEntity<?> updateOrderStatus(@RequestBody OrderStatusUpdateDto dto) {
        logger.info("Attempting to update order status for order ID: {}", dto.getOrderId());
        try {
            Order order = orderService.findById(dto.getOrderId());

            if (order == null) {
                logger.warn("Order not found with ID {} for status update.", dto.getOrderId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found.");
            }
            logger.debug("Fetched order for status update (ID {}): {}", dto.getOrderId(), order);

            order.setStatus(dto.getStatus());
            logger.debug("Updated order status to: {}", dto.getStatus());
            if (dto.getStatus() == OrderStatus.COMPLETED) {
                order.setCabinetNo(dto.getCabinetNo());
                logger.debug("Set cabinet number to: {}", dto.getCabinetNo());
            } else {
                order.setCabinetNo(null); // Clear cabinet number if status is not COMPLETED
                logger.debug("Cleared cabinet number as status is not COMPLETED.");
            }

            orderService.updateOrderStatus(order);
            logger.info("Order status updated successfully for ID: {}", dto.getOrderId());
            return ResponseEntity.ok("Order status updated successfully!");
        } catch (Exception e) {
            logger.error("An error occurred while updating order status for ID {}: {}", dto.getOrderId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating order status: " + e.getMessage());
        }
    }

    @PostMapping("/pickup")
    @ResponseBody
    public ResponseEntity<Map<String, String>> markOrderAsPickedUp(@RequestBody PickupRequest request) {
        logger.info("Attempting to mark order as picked up with ID: {} and amount received: {}", request.getOrderId(), request.getAmountReceived());
        return handleOrderRequest(request.getOrderId(), request.getAmountReceived(), "Order updated successfully.",
                orderService::markOrderAsPickedUp);
    }

    @GetMapping("/pending-payments")
    public String showPendingPayments(Model model) {
        logger.info("User accessed the pending payments page.");
        try {
            List<Order> pendingPayments = orderService.getOrdersWithOutstandingDue();
            logger.debug("Fetched {} orders with pending payments.", pendingPayments.size());

            model.addAttribute("pendingPayments", pendingPayments);
            model.addAttribute("activePage", "orders/pending-payments");
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            return "order/pending-payments";
        } catch (Exception e) {
            logger.error("Error fetching pending payments: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error fetching pending payments.");
            model.addAttribute("activePage", "orders/pending-payments");
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            return "order/pending-payments"; // Or handle differently
        }
    }

    @PostMapping("/pending-payments/pay")
    @ResponseBody
    public ResponseEntity<Map<String, String>> processPayment(@RequestBody PendingPaymentRequest request) {
        logger.info("Attempting to process payment of {} for order ID: {}", request.getPaymentAmount(), request.getOrderId());
        return handleOrderRequest(request.getOrderId(), request.getPaymentAmount(), "Payment successful.",
                orderService::processPayment);
    }

    /**
     * ✅ Utility method to handle common order request logic.
     */
    private ResponseEntity<Map<String, String>> handleOrderRequest(String encryptedOrderId, BigDecimal amount,
                                                                   String successMessage,
                                                                   BiConsumer<Order, BigDecimal> orderProcessor) {
        logger.debug("Handling order request for encrypted ID: {}, amount: {}, success message: {}", encryptedOrderId, amount, successMessage);
        try {
            Long decryptedOrderId = validation.validateAndFetchOrder(encryptedOrderId);
            if (decryptedOrderId == null) {
                logger.warn("Invalid order ID provided: {}", encryptedOrderId);
                return errorResponse(HttpStatus.NOT_FOUND, "Order not found!");
            }
            logger.debug("Decrypted order ID: {}", decryptedOrderId);

            Order order = orderService.findById(decryptedOrderId);
            if (order == null) {
                logger.warn("Order not found with ID: {}", decryptedOrderId);
                return errorResponse(HttpStatus.NOT_FOUND, "Order not found!");
            }
            logger.debug("Fetched order for processing (ID {}): {}", decryptedOrderId, order);

            // ✅ Process the order (payment or pickup)
            orderProcessor.accept(order, amount);
            logger.info("Order request processed successfully for ID: {}", decryptedOrderId);
            return ResponseEntity.ok(Collections.singletonMap("message", successMessage));
        } catch (IllegalArgumentException e) {
            logger.warn("Illegal argument exception during order request for ID {}: {}", encryptedOrderId, e.getMessage());
            return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("An unexpected error occurred during order request for ID {}: {}", encryptedOrderId, e.getMessage(), e);
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing request.");
        }
    }

    /**
     * ✅ Utility method to create error responses.
     */
    private ResponseEntity<Map<String, String>> errorResponse(HttpStatus status, String message) {
        logger.warn("Creating error response with status {} and message: {}", status, message);
        return ResponseEntity.status(status).body(Collections.singletonMap("error", message));
    }

}
