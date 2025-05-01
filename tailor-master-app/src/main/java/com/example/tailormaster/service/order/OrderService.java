package com.example.tailormaster.service.order;

import com.example.tailormaster.dto.OrderProductDto;
import com.example.tailormaster.dto.UpdateCustomerOrderDto;
import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.OrderProduct;
import com.example.tailormaster.entity.ledger.CustomerPaymentLedger;
import com.example.tailormaster.enums.OrderStatus;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.enums.PaymentType;
import com.example.tailormaster.enums.PickupStatus;
import com.example.tailormaster.repository.customerledger.CustomerPaymentRepository;
import com.example.tailormaster.repository.order.OrderRepository;
import com.example.tailormaster.repository.product.ProductRepository;
import com.example.tailormaster.util.ThymeleafUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final Logger logger = LogManager.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerPaymentRepository customerPaymentRepository;
    private final ThymeleafUtil thymeleafUtil;

    // Save an order
    public Order save(Order order) {
        try {
            Order savedOrder = orderRepository.save(order);

            // 1. Create a CREDIT entry for the full order amount
            CustomerPaymentLedger creditEntry = new CustomerPaymentLedger();
            creditEntry.setCustomer(savedOrder.getCustomer());
            creditEntry.setOrder(savedOrder);
            creditEntry.setAmount(savedOrder.getTotalProductAmount());
            creditEntry.setPaymentType(PaymentType.CREDIT); // assuming enum
            creditEntry.setRemarks("Order created#" + savedOrder.getOrderId());
            creditEntry.setPaymentDate(LocalDate.now());
            customerPaymentRepository.save(creditEntry);

            // 2. If advance is paid, create a DEBIT entry
            if (order.getAdvancePayment() != null && order.getAdvancePayment().compareTo(BigDecimal.ZERO) > 0) {
                CustomerPaymentLedger debitEntry = new CustomerPaymentLedger();
                debitEntry.setCustomer(savedOrder.getCustomer());
                debitEntry.setOrder(savedOrder);
                debitEntry.setAmount(savedOrder.getAdvancePayment());
                debitEntry.setPaymentType(PaymentType.DEBIT);
                debitEntry.setRemarks("Advance payment at order creation#" + savedOrder.getOrderId());
                debitEntry.setPaymentDate(LocalDate.now());
                customerPaymentRepository.save(debitEntry);
            }

            logger.info("Order saved with ID: {}", savedOrder.getId());
            return savedOrder;
        } catch (Exception e) {
            logger.error("Error saving order: {}", e.getMessage(), e);
            throw new RuntimeException("Error saving order.", e);
        }
    }

    public void updateOrderStatus(Order order) {
        try {
            Order savedOrder = orderRepository.save(order);
            logger.info("Order Status updated with ID: {}", savedOrder.getId());
        } catch (Exception e) {
            logger.error("Error Order Status update : {}", e.getMessage(), e);
            throw new RuntimeException("Error Order Status update.", e);
        }
    }

    // Get order by ID
    public Order findById(Long id) {
        try {
            Optional<Order> order = orderRepository.findById(id);
            return order.orElse(null);
        } catch (Exception e) {
            logger.error("Error finding order with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error finding order by ID.", e);
        }
    }

    public int getNextOrderNumberForUser(Long userId) {
        try {
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
                logger.warn("Error parsing last order number for user {}: {}", userId, e.getMessage());
                return 1; // Handle parsing error
            }
        } catch (Exception e) {
            logger.error("Error getting next order number for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Error getting next order number.", e);
        }
    }


    public Order updateOrder(Long orderId, UpdateCustomerOrderDto orderDto) {
        try {
            // Fetch the existing order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> {
                        logger.warn("Order not found with ID: {}", orderId);
                        return new EntityNotFoundException("Order not found with ID: " + orderId);
                    });

            BigDecimal oldTotal = order.getTotalProductAmount();
            BigDecimal newTotal = orderDto.getTotalProductAmount();

            BigDecimal oldAdvance = order.getAdvancePayment();
            BigDecimal newAdvance = orderDto.getAdvancePayment();

            Customer customer = order.getCustomer();

            // Update order details
            order.setOrderDate(orderDto.getOrderDate());
            order.setDeliveryDate(orderDto.getDeliveryDate());
            order.setTotalProductAmount(orderDto.getTotalProductAmount());
            order.setAdvancePayment(orderDto.getAdvancePayment());
            order.setDuePayment(orderDto.getDuePayment());

            // Map existing products by product ID for easy lookup
            Map<Long, OrderProduct> existingProductsMap = order.getOrderProducts().stream()
                    .collect(Collectors.toMap(op -> op.getProduct().getId(), op -> op));

            // Update product selections
            for (OrderProductDto productDto : orderDto.getOrderProducts()) {
                Product product = productRepository.findById(productDto.getId())
                        .orElseThrow(() -> {
                            logger.warn("Product not found with ID: {}", productDto.getId());
                            return new EntityNotFoundException("Product not found with ID: " + productDto.getId());
                        });

                if (existingProductsMap.containsKey(productDto.getId())) {
                    // Update existing order product
                    OrderProduct existingOrderProduct = existingProductsMap.get(productDto.getId());
                    existingOrderProduct.setQuantity(productDto.getQuantity());
                    existingOrderProduct.setSubtotal(productDto.getPrice().multiply(new BigDecimal(productDto.getQuantity()))); // Ensure this is calculated correctly
                } else {
                    // Add new product if it's not already in the order
                    OrderProduct newOrderProduct = new OrderProduct();
                    newOrderProduct.setOrder(order);
                    newOrderProduct.setProduct(product);
                    newOrderProduct.setQuantity(productDto.getQuantity());
                    newOrderProduct.setSubtotal(productDto.getPrice().multiply(new BigDecimal(productDto.getQuantity())));

                    order.getOrderProducts().add(newOrderProduct);
                }
            }

            // Save the updated order
            Order updatedOrder = orderRepository.save(order);

            // Record ledger entries
            if (newTotal.compareTo(oldTotal) > 0) {
                BigDecimal extraAmount = newTotal.subtract(oldTotal);
                customerPaymentRepository.save(new CustomerPaymentLedger(customer, order, extraAmount, LocalDate.now(), PaymentType.CREDIT, "Order total increased during update"));
            } else if (newTotal.compareTo(oldTotal) < 0) {
                BigDecimal refundAmount = oldTotal.subtract(newTotal);
                customerPaymentRepository.save(new CustomerPaymentLedger(customer, order, refundAmount, LocalDate.now(), PaymentType.DEBIT, "Order total decreased during update"));
            }

            if (newAdvance.compareTo(oldAdvance) > 0) {
                BigDecimal additionalPayment = newAdvance.subtract(oldAdvance);
                customerPaymentRepository.save(new CustomerPaymentLedger(customer, order, additionalPayment, LocalDate.now(), PaymentType.DEBIT, "Additional advance payment on order update"));
            }

            logger.info("Order with ID {} updated.", updatedOrder.getId());
            return updatedOrder;
        } catch (EntityNotFoundException e) {
            logger.warn("Entity not found while updating order {}: {}", orderId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error updating order with ID {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Error updating order.", e);
        }
    }

    public Map<String, Object> getPaginatedOrders(
            int draw,
            int start,
            int length,
            String searchValue,
            Integer columnIndex,
            String sortDirection,
            LocalDate startDate,
            LocalDate endDate,
            OrderStatus orderStatus,
            String paymentStatus
    ) {
        try {
            if (start < 0 || length <= 0) {
                throw new IllegalArgumentException("Start index and length must be greater than zero.");
            }

            String[] columnNames = {
                    "id", "orderId", "customer.fullName", "orderDate",
                    "deliveryDate", "status", "cabinetNo", "duePayment",
                    "pickupStatus", "paidAmount"
            };
            String sortBy = (columnIndex != null && columnIndex < columnNames.length)
                    ? columnNames[columnIndex]
                    : "orderDate";

            Sort sort = ("desc".equalsIgnoreCase(sortDirection))
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "orderDate")); // fetch more to allow in-memory pagination

            Page<Order> orderPage = orderRepository.findBySearchAndDateRangeAndStatus(
                    (searchValue != null && !searchValue.isEmpty()) ? searchValue : null,
                    startDate,
                    endDate,
                    orderStatus,
                    pageable
            );

            List<Order> filtered = orderPage.getContent().stream()
                    .filter(order -> matchesPaymentStatus(order, paymentStatus))
                    .toList();

            int endIdx = Math.min(start + length, filtered.size());
            List<Order> paginated = (start < filtered.size()) ? filtered.subList(start, endIdx) : List.of();

            List<Map<String, Object>> orderList = paginated.stream()
                    .map(this::mapOrderToResponse)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("draw", draw);
            response.put("recordsTotal", orderRepository.count());
            response.put("recordsFiltered", filtered.size());
            response.put("data", orderList);

            logger.info("Fetched paginated orders with custom payment filter - draw: {}, start: {}, length: {}, filtered: {}, total: {}",
                    draw, start, length, filtered.size(), orderRepository.count());

            return response;

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid pagination parameters: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error processing paginated orders request: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing paginated orders.", e);
        }
    }

    private boolean matchesPaymentStatus(Order order, String paymentStatus) {
        if (paymentStatus == null) return true;

        BigDecimal total = order.getTotalProductAmount();
        BigDecimal paid = order.getPaidAmount();
        PickupStatus pickupStatus = order.getPickupStatus();

        if (total == null || paid == null) return false;

        return switch (paymentStatus.toLowerCase()) {
            case "paid" -> paid.compareTo(total) == 0;
            case "partial_paid" -> pickupStatus == PickupStatus.PICKED_UP && paid.compareTo(total) < 0
                    && order.getStatus().equals(OrderStatus.COMPLETED);
            case "ready_for_pickup" -> pickupStatus == PickupStatus.NOT_PICKED_UP
                    && order.getStatus().equals(OrderStatus.COMPLETED);
            default -> true;
        };
    }

    private Map<String, Object> mapOrderToResponse(Order order) {
        Map<String, Object> orderMap = new HashMap<>();

        List<Map<String, Object>> orderProductList = getOrderProducts(order);

        orderMap.put("id", thymeleafUtil.encryptId(order.getId()));
        orderMap.put("encryptedId", thymeleafUtil.encryptId(order.getId()));
        orderMap.put("orderId", order.getOrderId());
        orderMap.put("customer", "<span>" + order.getCustomer().getFullName() + "</span><br>" +
                "<small class='text-muted'>" + order.getCustomer().getPhoneNumber() + "</small>");
        orderMap.put("orderDate", order.getOrderDate());
        orderMap.put("deliveryDate", order.getDeliveryDate());
        orderMap.put("status", order.getStatus().name());
        orderMap.put("cabinetNo", order.getCabinetNo());
        orderMap.put("advancePayment", order.getAdvancePayment());
        orderMap.put("duePayment", order.getDuePayment());
        orderMap.put("totalProductAmount", order.getTotalProductAmount());
        orderMap.put("paidAmount", order.getPaidAmount());
        orderMap.put("pickupStatus", order.getPickupStatus() != null
                ? order.getPickupStatus().name()
                : "NOT_PICKED_UP");
        orderMap.put("orderProducts", orderProductList);

        return orderMap;
    }


    public List<Map<String, Object>> getOrderProducts(Order order) {
        // get order products
        List<OrderProduct> orderProducts = order.getOrderProducts();
        return orderProducts.stream().map(op -> {
            Map<String, Object> opMap = new HashMap<>();
            opMap.put("productName", op.getProduct().getName());
            opMap.put("quantity", op.getQuantity());
            opMap.put("price", op.getProduct().getPrice());
            opMap.put("subtotal", op.getSubtotal());
            return opMap;
        }).toList();
    }

    @Transactional
    public void markOrderAsPickedUp(Order order, BigDecimal amountReceived) {
        logger.info("Attempting the Order: {} mark as picked up with amount received: {}", order, amountReceived);
        try {
            BigDecimal dueAmount = order.getDuePayment();
            BigDecimal advancePayment = order.getAdvancePayment();

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
            order.setPaidAmount(advancePayment.add(amountReceived.max(BigDecimal.ZERO)));

            orderRepository.save(order);

            // ✅ Insert payment entry for amount received (CREDIT)
            CustomerPaymentLedger payment = new CustomerPaymentLedger();
            payment.setCustomer(order.getCustomer());
            payment.setOrder(order);
            payment.setPaymentType(PaymentType.DEBIT); // CREDIT means payment received

            if (amountReceived.compareTo(dueAmount) == 0) {
                payment.setRemarks("Full payment received at pickup");
            } else {
                payment.setRemarks("Partial payment received at pickup");
            }

            payment.setAmount(amountReceived);
            payment.setPaymentDate(LocalDate.now());
            customerPaymentRepository.save(payment);

            // ✅ Insert debit entry if any outstanding due remains (DEBIT)
//            if (outstandingDueAmount.compareTo(BigDecimal.ZERO) > 0) {
//                CustomerPaymentLedger dueEntry = new CustomerPaymentLedger();
//                dueEntry.setCustomer(order.getCustomer());
//                dueEntry.setOrder(order);
//                dueEntry.setPaymentType(PaymentType.CREDIT); // DEBIT means customer still owes
//                dueEntry.setRemarks("Remaining due after partial pickup payment");
//                dueEntry.setAmount(outstandingDueAmount);
//                dueEntry.setPaymentDate(LocalDate.now());
//                customerPaymentRepository.save(dueEntry);
//
//            }

            logger.info("Order with ID {} marked as picked up. Amount received: {}, Outstanding due: {}", order.getId(), amountReceived, outstandingDueAmount);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid input for marking order {} as picked up: {}", order.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error marking order {} as picked up: {}", order.getId(), e.getMessage(), e);
            throw new RuntimeException("Error marking order as picked up.", e);
        }
    }

    public List<Order> getOrdersWithOutstandingDue() {
        try {
            List<Order> orders = orderRepository.findOrdersWithOutstandingDue();
            logger.debug("Fetched {} orders with outstanding due.", orders.size());
            return orders;
        } catch (Exception e) {
            logger.error("Error fetching orders with outstanding due: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching orders with outstanding due.", e);
        }
    }

    @Transactional
    public void processPayment(Order order, BigDecimal paymentAmount) {
        logger.info("Attempting the process payment for Order: {}", order);
        try {
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

            Order updatedOrder = orderRepository.save(order);

            // ✅ Insert CREDIT entry (payment received)
            CustomerPaymentLedger payment = new CustomerPaymentLedger();
            payment.setCustomer(order.getCustomer());
            payment.setOrder(order);
            payment.setPaymentType(PaymentType.DEBIT);
            payment.setRemarks("Outstanding due amount received.");
            payment.setAmount(paymentAmount);
            payment.setPaymentDate(LocalDate.now());
            customerPaymentRepository.save(payment);

            logger.info("Payment of {} processed for Order: {}", paymentAmount, updatedOrder);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid payment amount for order {}: {}", order.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error processing payment for order {}: {}", order.getId(), e.getMessage(), e);
            throw new RuntimeException("Error processing payment.", e);
        }
    }

    public Long getTotalOrdersCount() {
        try {
            long count = orderRepository.count();
            logger.debug("Total orders count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting total orders count: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting total orders count.", e);
        }
    }

    public Long getPendingPaymentsCount() {
        try {
            long count = orderRepository.countPendingPayments();
            logger.debug("Pending payments count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting pending payments count: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting pending payments count.", e);
        }
    }

    public Long getOrdersInProgressCount() {
        try {
            long count = orderRepository.countOrdersInProgress();
            logger.debug("Orders in progress count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting orders in progress count: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting orders in progress count.", e);
        }
    }

    public Long getCompletedOrdersCount() {
        try {
            long count = orderRepository.countCompletedOrders();
            logger.debug("Completed orders count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting completed orders count: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting completed orders count.", e);
        }
    }

    public Long getOrdersReadyForPickupCount() {
        try {
            long count = orderRepository.countOrdersReadyForPickup();
            logger.debug("Orders ready for pickup count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting orders ready for pickup count: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting orders ready for pickup count.", e);
        }
    }

    public Map<String, Long> getOrderStatusCounts() {
        try {
            Map<String, Long> statusCounts = new LinkedHashMap<>();
            statusCounts.put("Cancelled", orderRepository.countCancelledOrders());
            statusCounts.put("Pending", orderRepository.countPendingOrders());
            statusCounts.put("InProgress", orderRepository.countOrdersInProgress());
            statusCounts.put("Completed", orderRepository.countCompletedOrders());
            statusCounts.put("Ready for Pickup", orderRepository.countOrdersReadyForPickup());
            logger.debug("Fetched order status counts: {}", statusCounts);
            return statusCounts;
        } catch (Exception e) {
            logger.error("Error fetching order status counts: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching order status counts.", e);
        }
    }

    public List<Order> getTop5ByOrderByCreatedAtDesc() {
        try {
            List<Order> top5Orders = orderRepository.findTop5ByOrderByCreatedAtDesc();
            logger.debug("Fetched top 5 recent orders.");
            return top5Orders;
        } catch (Exception e) {
            logger.error("Error fetching top 5 recent orders: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching top 5 recent orders.", e);
        }
    }

    public List<Map<String, Object>> getTopCustomers() {
        try {
            Pageable topFive = PageRequest.of(0, 5);
            List<Object[]> results = orderRepository.findTopCustomersWithDetails(topFive);

            List<Map<String, Object>> topCustomers = results.stream().map(obj -> {
                Map<String, Object> map = new HashMap<>();
                map.put("name", obj[0]);
                map.put("phone", obj[1]);
                map.put("totalOrders", obj[2]);
                map.put("totalPaid", obj[3]);
                return map;
            }).collect(Collectors.toList());
            logger.debug("Fetched top {} customers.", topCustomers.size());
            return topCustomers;
        } catch (Exception e) {
            logger.error("Error fetching top customers: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching top customers.", e);
        }
    }

    public List<Map<String, Object>> getUpcomingDeliveries() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Karachi"));
        LocalDate threeDaysLater = today.plusDays(3);

        try {
            List<OrderStatus> excludedStatuses = Arrays.asList(OrderStatus.CANCELLED, OrderStatus.COMPLETED);

            // Fetch orders that are either upcoming or overdue but still not completed
            List<Order> upcomingOrOverdueOrders = orderRepository
                    .findByDeliveryDateLessThanEqualAndStatusNotIn(threeDaysLater, excludedStatuses);

            // Optional: sort by delivery date ascending
            upcomingOrOverdueOrders.sort(Comparator.comparing(Order::getDeliveryDate));

            List<Map<String, Object>> deliveries = upcomingOrOverdueOrders.stream().map(order -> {
                Map<String, Object> map = new HashMap<>();
                map.put("orderId", order.getOrderId());
                map.put("orderId_pk", thymeleafUtil.encryptId(order.getId()));
                map.put("customerName", order.getCustomer().getFullName());
                map.put("deliveryDate", order.getDeliveryDate().toString());
                map.put("status", order.getStatus().name());
                map.put("isOverdue", order.getDeliveryDate().isBefore(today));
                return map;
            }).collect(Collectors.toList());
            logger.debug("Fetched {} upcoming deliveries.", deliveries.size());
            return deliveries;
        } catch (Exception e) {
            logger.error("Error fetching upcoming deliveries: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching upcoming deliveries.", e);
        }
    }
}
