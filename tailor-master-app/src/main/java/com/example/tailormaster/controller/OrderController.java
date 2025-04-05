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

    private final CustomerService customerService;
    private final ProductService productService;
    private final CustomerMeasurementService customerMeasurementService;
    private final OrderService orderService;
    private final UserService userService;
    private final Validation validation;

    @GetMapping
    public String listOrders(Model model) {
        model.addAttribute("thymeleafUtil", new ThymeleafUtil());
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
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            Map<String, Object> response = orderService.getPaginatedOrders(draw, start, length, searchValue, columnIndex, sortDirection, startDate, endDate);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(createErrorResponse("Invalid request parameters: " + e.getMessage()));
        } catch (Exception e) {
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

        try {
            // Decrypt and validate customer ID
            Long customerId = validation.validateAndFetchCustomer(id, redirectAttributes);
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
        try {
            // Decrypt and validate customer ID
            Long customerId = validation.validateAndFetchCustomer(encryptedCustomerId, redirectAttributes);
            if (customerId == null) {
                return "redirect:/customers";
            }

            // Fetch customer details (since ID is valid)
            Customer customer = customerService.getCustomerById(customerId);

            // Perform validation checks
            validateOrder(orderDto, result);
            if (result.hasErrors()) {
                model.addAttribute("org.springframework.validation.BindingResult.orderDto", result);
                return populateModel(model, customerId, orderDto, customer);
            }

            Optional<User> user = userService.findByUsername(principal.getName());
            if (user.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
                return "redirect:/customers";
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

        if (result.hasErrors()) {return;}

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
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid order ID.");
                return "redirect:/orders";
            }

            Order order = orderService.findById(orderId);
            model.addAttribute("order", order);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());

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

    @GetMapping("/edit/{id}")
    public String showEditOrderForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        try {
            // Decrypt and validate order ID
            Long orderId = validation.validateAndFetchOrder(id, redirectAttributes);
            if (orderId == null) {
                return "redirect:/orders";
            }

            Order order = orderService.findById(orderId);
            UpdateCustomerOrderDto orderUpdateDto = populateOrderUpdateDto(order);
            model.addAttribute("orderDto", orderUpdateDto);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            return "order/update";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating order: " + e.getMessage());
            return "redirect:/orders"; // Keep only one return statement
        }
    }

    private UpdateCustomerOrderDto populateOrderUpdateDto(Order order) {
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

        return orderUpdateDto;
    }

    @PostMapping("/update")
    public String updateOrder(
            @ModelAttribute("orderDto") @Valid UpdateCustomerOrderDto orderUpdateDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            @RequestParam("encryptedOrderId") String encryptedOrderId) {

        try {
            // Decrypt and validate order ID
            Long orderId = validation.validateAndFetchOrder(encryptedOrderId, redirectAttributes);
            if (orderId == null) {
                return "redirect:/orders";
            }

            // Fetch order details (since ID is valid)
            Order order = orderService.findById(orderId);

            // Perform validation checks
            validateOrder(orderUpdateDto, bindingResult);
            if (bindingResult.hasErrors()) {
                model.addAttribute("org.springframework.validation.BindingResult.orderDto", bindingResult);
                UpdateCustomerOrderDto orderUpdateDto1 = populateOrderUpdateDto(order);
                model.addAttribute("orderDto", orderUpdateDto1);
                model.addAttribute("thymeleafUtil", new ThymeleafUtil());
                return "order/update";
            }

            Order updateOrder = orderService.updateOrder(orderId, orderUpdateDto);

            redirectAttributes.addFlashAttribute("successMessage", "Order updated successfully!");
            redirectAttributes.addFlashAttribute("orderId", updateOrder.getId());
            return "redirect:/orders";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating an order, please try again.");
            return "redirect:/orders";
        }
    }

    @PostMapping("/update-order-status")
    @ResponseBody
    public ResponseEntity<?> updateOrderStatus(@RequestBody OrderStatusUpdateDto dto) {
        try {
            Order order = orderService.findById(dto.getOrderId());

            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found.");
            }

            order.setStatus(dto.getStatus());

            if (dto.getStatus() == OrderStatus.COMPLETED) {
                order.setCabinetNo(dto.getCabinetNo());
            } else {
                order.setCabinetNo(null); // Clear cabinet number if status is not COMPLETED
            }

            orderService.save(order);
            return ResponseEntity.ok("Order status updated successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating order status: " + e.getMessage());
        }
    }

    @PostMapping("/pickup")
    @ResponseBody
    public ResponseEntity<Map<String, String>> markOrderAsPickedUp(@RequestBody PickupRequest request) {
        return handleOrderRequest(request.getOrderId(), request.getAmountReceived(), "Order updated successfully.",
                orderService::markOrderAsPickedUp);
    }

    @GetMapping("/pending-payments")
    public String showPendingPayments(Model model) {
        List<Order> pendingPayments = orderService.getOrdersWithOutstandingDue();
        model.addAttribute("pendingPayments", pendingPayments);
        model.addAttribute("activePage", "orders/pending-payments");
        model.addAttribute("thymeleafUtil", new ThymeleafUtil());
        return "order/pending-payments";
    }

    @PostMapping("/pending-payments/pay")
    @ResponseBody
    public ResponseEntity<Map<String, String>> processPayment(@RequestBody PendingPaymentRequest request) {
        return handleOrderRequest(request.getOrderId(), request.getPaymentAmount(), "Payment successful.",
                orderService::processPayment);
    }

    /**
     * ✅ Utility method to handle common order request logic.
     */
    private ResponseEntity<Map<String, String>> handleOrderRequest(String encryptedOrderId, BigDecimal amount,
                                                                   String successMessage,
                                                                   BiConsumer<Order, BigDecimal> orderProcessor) {
        try {
            Long decryptedOrderId = validation.validateAndFetchOrder(encryptedOrderId);
            if (decryptedOrderId == null) {
                return errorResponse(HttpStatus.NOT_FOUND, "Order not found!");
            }

            Order order = orderService.findById(decryptedOrderId);
            if (order == null) {
                return errorResponse(HttpStatus.NOT_FOUND, "Order not found!");
            }

            // ✅ Process the order (payment or pickup)
            orderProcessor.accept(order, amount);

            return ResponseEntity.ok(Collections.singletonMap("message", successMessage));
        } catch (IllegalArgumentException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing request.");
        }
    }

    /**
     * ✅ Utility method to create error responses.
     */
    private ResponseEntity<Map<String, String>> errorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Collections.singletonMap("error", message));
    }

}
