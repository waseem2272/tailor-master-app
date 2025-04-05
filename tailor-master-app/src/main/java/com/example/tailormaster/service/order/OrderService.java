package com.example.tailormaster.service.order;

import com.example.tailormaster.dto.OrderProductDto;
import com.example.tailormaster.dto.UpdateCustomerOrderDto;
import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.OrderProduct;
import com.example.tailormaster.enums.OrderStatus;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.enums.PickupStatus;
import com.example.tailormaster.repository.order.OrderRepository;
import com.example.tailormaster.repository.product.ProductRepository;
import com.example.tailormaster.util.ThymeleafUtil;
import com.example.tailormaster.validation.Validation;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ThymeleafUtil thymeleafUtil;

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

    public Map<String, Object> getPaginatedOrders(
            int draw,
            int start,
            int length,
            String searchValue,
            Integer columnIndex,
            String sortDirection,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (start < 0 || length <= 0) {
            throw new IllegalArgumentException("Start index and length must be greater than zero.");
        }

        String[] columnNames = {"id", "orderId", "customer.fullName", "orderDate", "deliveryDate", "status", "cabinetNo", "duePayment", "pickupStatus", "paidAmount"};
        String sortBy = (columnIndex != null && columnIndex < columnNames.length) ? columnNames[columnIndex] : "orderDate";

        Sort sort = (sortDirection != null && sortDirection.equalsIgnoreCase("desc"))
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(start / length, length, sort);

        Page<Order> orderPage;
        try {
            orderPage = orderRepository.findBySearchAndDateRange(
                    (searchValue != null && !searchValue.isEmpty()) ? searchValue : null,
                    startDate,
                    endDate,
                    pageable
            );
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving orders from the database.", e);
        }

        List<Map<String, Object>> orderList = orderPage.getContent().stream().map(order -> {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("orderId", order.getOrderId());
            orderMap.put("customer", "<span>" + order.getCustomer().getFullName() + "</span><br>" +
                    "<small class='text-muted'>" + order.getCustomer().getPhoneNumber() + "</small>");
            orderMap.put("orderDate", order.getOrderDate());
            orderMap.put("deliveryDate", order.getDeliveryDate());
            orderMap.put("status", order.getStatus().name());
            orderMap.put("cabinetNo", order.getCabinetNo());
            orderMap.put("id", thymeleafUtil.encryptId(order.getId()));
            orderMap.put("encryptedId", thymeleafUtil.encryptId(order.getId()));
            orderMap.put("duePayment", order.getDuePayment());
            orderMap.put("paidAmount", order.getPaidAmount());
            orderMap.put("pickupStatus", order.getPickupStatus() != null ? order.getPickupStatus().name() : "NOT_PICKED_UP");
            return orderMap;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("draw", draw);
        response.put("recordsTotal", orderRepository.count());
        response.put("recordsFiltered", orderPage.getTotalElements());
        response.put("data", orderList);

        return response;
    }

    @Transactional
    public void markOrderAsPickedUp(Order order, BigDecimal amountReceived) {

        BigDecimal dueAmount = order.getDuePayment();

        // ✅ Validate amount received
        if (amountReceived.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount received must be greater than 0.");
        }
        if (amountReceived.compareTo(dueAmount) > 0) {
            throw new IllegalArgumentException("Amount received cannot be greater than due.");
        }

        // ✅ Calculate Outstanding Due
        BigDecimal outstandingDueAmount = dueAmount.subtract(amountReceived).max(BigDecimal.ZERO);

        order.setPickupStatus(PickupStatus.PICKED_UP);
        order.setPickedUpWithDue(outstandingDueAmount.compareTo(BigDecimal.ZERO) > 0); // TRUE if any due remains
        order.setPickupDate(LocalDateTime.now());
        order.setStatus(OrderStatus.COMPLETED);

        // ✅ Update the outstanding due amount field
        order.setOutstandingDueAmount(outstandingDueAmount);
        order.setPaidAmount(amountReceived.max(BigDecimal.ZERO));

        orderRepository.save(order);
    }

    public List<Order> getOrdersWithOutstandingDue() {
        return orderRepository.findOrdersWithOutstandingDue();
    }

    @Transactional
    public void processPayment(Order order, BigDecimal paymentAmount) {

        BigDecimal outstandingDue = order.getOutstandingDueAmount();

        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0.");
        }
        if (paymentAmount.compareTo(outstandingDue) > 0) {
            throw new IllegalArgumentException("Payment amount cannot exceed outstanding due.");
        }

        // Process payment
        order.setPaidAmount(order.getPaidAmount().add(paymentAmount));
        order.setOutstandingDueAmount(outstandingDue.subtract(paymentAmount));

        if (order.getOutstandingDueAmount().compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus(OrderStatus.COMPLETED);
        }

        orderRepository.save(order);
    }

    public Long getTotalOrdersCount() {
        return orderRepository.count();
    }

    public Long getPendingPaymentsCount() {
        return orderRepository.countPendingPayments();
    }

    public Long getOrdersInProgressCount() {
        return orderRepository.countOrdersInProgress();
    }

    public Long getCompletedOrdersCount() {
        return orderRepository.countCompletedOrders();
    }

    public Long getOrdersReadyForPickupCount() {
        return orderRepository.countOrdersReadyForPickup();
    }

    public BigDecimal getRevenueThisMonth() {
        return orderRepository.getRevenueThisMonth();
    }

    public List<Map<String, Object>> getMonthlyRevenueTrendsForLast6Months() {
        List<Map<String, Object>> trends = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Karachi"));

        for (int i = 5; i >= 0; i--) {
            YearMonth yearMonth = YearMonth.from(today.minusMonths(i));
            LocalDate firstDayOfMonth = yearMonth.atDay(1);
            LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();

            LocalDateTime startOfMonth = firstDayOfMonth.atStartOfDay(ZoneId.of("Asia/Karachi")).toLocalDateTime();
            LocalDateTime endOfMonth = lastDayOfMonth.atTime(23, 59, 59, 999999999).atZone(ZoneId.of("Asia/Karachi")).toLocalDateTime();

            List<Order> completedOrders = orderRepository.findByPickupDateBetweenAndStatusAndPickupStatus(startOfMonth, endOfMonth, OrderStatus.COMPLETED, PickupStatus.PICKED_UP); // Adjust status as needed
            BigDecimal monthlyRevenue = completedOrders.stream()
                    .map(Order::getPaidAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("month", yearMonth.format(DateTimeFormatter.ofPattern("MMM yyyy"))); // Format month display
            dataPoint.put("revenue", monthlyRevenue);
            trends.add(dataPoint);
        }
        return trends;
    }

    public Map<String, Long> getOrderStatusCounts() {
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        statusCounts.put("Pending", orderRepository.countPendingOrders());
        statusCounts.put("InProgress", orderRepository.countOrdersInProgress());
        statusCounts.put("Completed", orderRepository.countCompletedOrders());
        statusCounts.put("Ready for Pickup", orderRepository.countOrdersReadyForPickup());
        return statusCounts;
    }

    // In OrderService.java
    public List<Order> getRecentOrders(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findAll(pageable).getContent();
    }

    public List<Order> getTop5ByOrderByCreatedAtDesc() {
        return orderRepository.findTop5ByOrderByCreatedAtDesc();
    }

    public List<Map<String, Object>> getTopCustomers() {
        Pageable topFive = PageRequest.of(0, 5);
        List<Object[]> results = orderRepository.findTopCustomersWithDetails(topFive);

        return results.stream().map(obj -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", obj[0]);
            map.put("phone", obj[1]);
            map.put("totalOrders", obj[2]);
            map.put("totalPaid", obj[3]);
            return map;
        }).collect(Collectors.toList());
    }

}
