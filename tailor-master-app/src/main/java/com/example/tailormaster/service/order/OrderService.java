package com.example.tailormaster.service.order;

import com.example.tailormaster.dto.OrderProductDto;
import com.example.tailormaster.dto.OrderProductStatusUpdateDto;
import com.example.tailormaster.dto.OrderStatusUpdateDto;
import com.example.tailormaster.dto.UpdateCustomerOrderDto;
import com.example.tailormaster.entity.*;
import com.example.tailormaster.entity.ledger.CustomerPaymentLedger;
import com.example.tailormaster.enums.*;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.repository.customerledger.CustomerPaymentRepository;
import com.example.tailormaster.repository.order.OrderRepository;
import com.example.tailormaster.repository.orderproduct.OrderProductRepository;
import com.example.tailormaster.repository.product.ProductRepository;
import com.example.tailormaster.service.InventoryItemService;
import com.example.tailormaster.service.OrderInventoryService;
import com.example.tailormaster.util.AuthenticatedUserService;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class OrderService {

    private static final Logger logger = LogManager.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerPaymentRepository customerPaymentRepository;
    private final ThymeleafUtil thymeleafUtil;
    private final AuthenticatedUserService authenticatedUserService;
    private final OrderInventoryService orderInventoryService;
    private final OrderProductRepository orderProductRepository;
    private final InventoryItemService inventoryItemService;

    // Save an order
    @Transactional
    public Order save(Order order) {
        User currentUser = authenticatedUserService.getCurrentUser();
        order.setUser(currentUser);

        Order savedOrder = orderRepository.save(order);
        BigDecimal finalTotal = BigDecimal.ZERO;

        for (OrderProduct orderProduct : savedOrder.getOrderProducts()) {

            if (orderProduct.getOrderProductType() == OrderProductType.INVENTORY) {

                if (orderProduct.getInventoryItem() == null) {
                    throw new IllegalStateException(
                            "Inventory item is missing for inventory order product."
                    );
                }

                BigDecimal quantity = BigDecimal.valueOf(orderProduct.getQuantity());

                BigDecimal inventoryAmount = orderInventoryService.reserveInventory(
                        savedOrder,
                        orderProduct,
                        orderProduct.getInventoryItem().getId(),
                        quantity
                );

                BigDecimal inventoryUnitPrice = quantity.compareTo(BigDecimal.ZERO) > 0
                        ? inventoryAmount.divide(quantity, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                BigDecimal alterationFee = orderProduct.isAlterationRequired()
                        ? (orderProduct.getAlterationFee() != null
                        ? orderProduct.getAlterationFee()
                        : BigDecimal.ZERO)
                        : BigDecimal.ZERO;

                if (orderProduct.isAlterationRequired()) {
                    orderProduct.setStatus(OrderProductStatus.PENDING_FOR_ALTERATION);
                } else {
                    orderProduct.setStatus(OrderProductStatus.INSTANT_DELIVERED);
                }

                BigDecimal subtotal = inventoryAmount.add(alterationFee);

                orderProduct.setInventoryAmount(inventoryAmount);
                orderProduct.setInventoryUnitPrice(inventoryUnitPrice);
                orderProduct.setAlterationFee(alterationFee);
                orderProduct.setSubtotal(subtotal);

                orderProductRepository.save(orderProduct);

                if (!orderProduct.isAlterationRequired()) {
                    orderInventoryService.consumeInventoryItem(
                            savedOrder,
                            orderProduct,
                            currentUser
                    );
                }

                finalTotal = finalTotal.add(subtotal);

                logger.info(
                        "Inventory order product consumed. orderId={}, orderProductId={}, quantity={}, inventoryAmount={}, alterationRequired={}, alterationFee={}, subtotal={}, status={}, unitPrice={}",
                        savedOrder.getOrderId(),
                        orderProduct.getId(),
                        quantity,
                        inventoryAmount,
                        orderProduct.isAlterationRequired(),
                        alterationFee,
                        subtotal,
                        orderProduct.getStatus(),
                        inventoryUnitPrice
                );

                continue;
            }

            BigDecimal stitchingAmount = orderProduct.getSilaiAmount() != null
                    ? orderProduct.getSilaiAmount()
                    .multiply(BigDecimal.valueOf(orderProduct.getQuantity()))
                    : BigDecimal.ZERO;

            BigDecimal fabricAmount = BigDecimal.ZERO;

            if (orderProduct.getFabricSource() == FabricSource.SHOP &&
                    orderProduct.getInventoryItem() != null &&
                    orderProduct.getFabricQuantity() != null) {

                fabricAmount = orderInventoryService.reserveInventory(
                        savedOrder,
                        orderProduct,
                        orderProduct.getInventoryItem().getId(),
                        orderProduct.getFabricQuantity()
                );

                BigDecimal fabricUnitPrice = orderProduct.getFabricQuantity()
                        .compareTo(BigDecimal.ZERO) > 0
                        ? fabricAmount.divide(
                        orderProduct.getFabricQuantity(),
                        2,
                        RoundingMode.HALF_UP
                )
                        : BigDecimal.ZERO;

                orderProduct.setFabricAmount(fabricAmount);
                orderProduct.setFabricUnitPrice(fabricUnitPrice);

                logger.info(
                        "Shop fabric reserved. orderId={}, orderProductId={}, quantity={}, amount={}, unitPrice={}",
                        savedOrder.getOrderId(),
                        orderProduct.getId(),
                        orderProduct.getFabricQuantity(),
                        fabricAmount,
                        fabricUnitPrice
                );
            }

            BigDecimal subtotal = stitchingAmount.add(fabricAmount);

            orderProduct.setSubtotal(subtotal);
            orderProductRepository.save(orderProduct);

            finalTotal = finalTotal.add(subtotal);
        }

        BigDecimal advance = savedOrder.getAdvancePayment() != null
                ? savedOrder.getAdvancePayment()
                : BigDecimal.ZERO;

        BigDecimal due = finalTotal.subtract(advance);

        savedOrder.setTotalProductAmount(finalTotal);
        // Advance paid at order creation is the paid amount
        savedOrder.setPaidAmount(advance);
        savedOrder.setDuePayment(due);

        savedOrder = orderRepository.save(savedOrder);

        CustomerPaymentLedger creditEntry = new CustomerPaymentLedger();
        creditEntry.setCustomer(savedOrder.getCustomer());
        creditEntry.setOrder(savedOrder);
        creditEntry.setAmount(finalTotal);
        creditEntry.setPaymentType(PaymentType.CREDIT);
        creditEntry.setRemarks("Order created#" + savedOrder.getOrderId());
        creditEntry.setPaymentDate(LocalDate.now());
        customerPaymentRepository.save(creditEntry);

        if (advance.compareTo(BigDecimal.ZERO) > 0) {
            CustomerPaymentLedger debitEntry = new CustomerPaymentLedger();
            debitEntry.setCustomer(savedOrder.getCustomer());
            debitEntry.setOrder(savedOrder);
            debitEntry.setAmount(advance);
            debitEntry.setPaymentType(PaymentType.DEBIT);
            debitEntry.setRemarks(
                    "Advance payment at order creation#" + savedOrder.getOrderId()
            );
            debitEntry.setPaymentDate(LocalDate.now());
            customerPaymentRepository.save(debitEntry);
        }

        logger.info(
                "Order saved successfully with FIFO inventory reservation. orderId={}, orderPkId={}, total={}, advance={}, due={}",
                savedOrder.getOrderId(),
                savedOrder.getId(),
                finalTotal,
                advance,
                due
        );

        return savedOrder;
    }

    @Transactional
    public void updateOrderStatus(OrderStatusUpdateDto orderStatusUpdateDto) {
        try {
            User currentUser = authenticatedUserService.getCurrentUser();
            if (orderStatusUpdateDto == null || orderStatusUpdateDto.getOrderId() == null) {
                throw new IllegalArgumentException("Order ID is required.");
            }
            if (orderStatusUpdateDto.getStatus() == null) {
                throw new IllegalArgumentException("Order status is required.");
            }
            Order existingOrder = orderRepository.findByIdAndUser(orderStatusUpdateDto.getOrderId(), currentUser).orElseThrow(() -> new IllegalArgumentException("Order not found."));
            OrderStatus oldStatus = existingOrder.getStatus();
            OrderStatus newStatus = orderStatusUpdateDto.getStatus();
            if (oldStatus == null) {
                throw new IllegalStateException("Current order status is missing.");
            }
            logger.info("Updating order status. orderId={}, oldStatus={}, newStatus={}", existingOrder.getOrderId(), oldStatus, newStatus);
            if (oldStatus == newStatus) {
                logger.info("Order status is already {}. No status transition required. orderId={}", newStatus, existingOrder.getOrderId());
                if (newStatus == OrderStatus.COMPLETED) {
                    existingOrder.setCabinetNo(orderStatusUpdateDto.getCabinetNo());
                } else {
                    existingOrder.setCabinetNo(null);
                }
                existingOrder.setUser(currentUser);
                orderRepository.save(existingOrder);
                return;
            }
            boolean validTransition = (oldStatus == OrderStatus.PENDING && (newStatus == OrderStatus.IN_PROGRESS || newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.CANCELLED)) || (oldStatus == OrderStatus.IN_PROGRESS && newStatus == OrderStatus.COMPLETED);
            if (!validTransition) {
                throw new IllegalArgumentException("Invalid order status transition: " + oldStatus + " -> " + newStatus);
            } /* * PENDING -> IN_PROGRESS / COMPLETED * * Shop fabric is consumed when tailoring work starts * or when the order is directly completed. */
            if (oldStatus == OrderStatus.PENDING && (newStatus == OrderStatus.IN_PROGRESS || newStatus == OrderStatus.COMPLETED)) {
                logger.info("Consuming shop fabric stock for order {}", existingOrder.getOrderId());
                orderInventoryService.consumeShopFabric(existingOrder);
            } /* * IN_PROGRESS -> COMPLETED * * No additional inventory consumption is required. */
            if (oldStatus == OrderStatus.IN_PROGRESS && newStatus == OrderStatus.COMPLETED) {
                logger.info("Completing order. orderId={}", existingOrder.getOrderId());
            } /* * PENDING -> CANCELLED * * Shop fabric was only reserved at order creation, * so release the reservation. */
            if (oldStatus == OrderStatus.PENDING && newStatus == OrderStatus.CANCELLED) {
                logger.info(
                        "Releasing inventory reservations for cancelled order {}",
                        existingOrder.getOrderId()
                );

                for (OrderProduct orderProduct : existingOrder.getOrderProducts()) {
                    if (orderProduct.getStatus() == OrderProductStatus.PENDING_FOR_ALTERATION) {
                        orderInventoryService.releaseOrderProductReservation(orderProduct);
                    }
                }

                orderInventoryService.reverseShopFabric(existingOrder);
            }
            existingOrder.setStatus(newStatus);
            if (newStatus == OrderStatus.COMPLETED) {
                existingOrder.setCabinetNo(orderStatusUpdateDto.getCabinetNo());
            } else {
                existingOrder.setCabinetNo(null);
            }
            existingOrder.setUser(currentUser);
            orderRepository.save(existingOrder);
            logger.info("Order status updated successfully. orderId={}, oldStatus={}, newStatus={}", existingOrder.getOrderId(), oldStatus, newStatus);
        } catch (Exception e) {
            logger.error("Error while updating order status. orderId={}", orderStatusUpdateDto != null ? orderStatusUpdateDto.getOrderId() : null, e);
            throw e;
        }
    }

    @Transactional
    public void updateOrderProductStatus(OrderProductStatusUpdateDto dto) {
        try {
            if (dto == null || dto.getOrderProductId() == null) {
                throw new IllegalArgumentException("Order product ID is required.");
            }

            if (dto.getStatus() == null) {
                throw new IllegalArgumentException("Product status is required.");
            }

            User currentUser = authenticatedUserService.getCurrentUser();

            OrderProduct orderProduct = orderProductRepository
                    .findByIdAndOrderUser(dto.getOrderProductId(), currentUser)
                    .orElseThrow(() ->
                            new IllegalArgumentException("Order product not found.")
                    );

            Order order = orderProduct.getOrder();

            if (order == null) {
                throw new IllegalStateException("Order is missing for this product.");
            }

            if (order.getStatus() == OrderStatus.CANCELLED ||
                    order.getStatus() == OrderStatus.DELIVERED) {
                throw new IllegalArgumentException(
                        "Product status cannot be changed after order is cancelled or delivered."
                );
            }

            OrderProductType productType = orderProduct.getOrderProductType();

            if (productType == null) {
                throw new IllegalStateException(
                        "Order product type is missing."
                );
            }

            OrderProductStatus oldStatus = orderProduct.getStatus();
            OrderProductStatus newStatus = dto.getStatus();

            if (oldStatus == null) {
                throw new IllegalStateException(
                        "Current product status is missing."
                );
            }

            if (oldStatus == OrderProductStatus.CANCELLED) {
                if (newStatus == OrderProductStatus.CANCELLED) {
                    return;
                }

                throw new IllegalArgumentException(
                        "Cancelled product status cannot be changed."
                );
            }

            /*
             * =========================================================
             * INVENTORY PRODUCT
             * =========================================================
             */
            if (productType == OrderProductType.INVENTORY) {

                if (!orderProduct.isAlterationRequired()) {

                    if (oldStatus != OrderProductStatus.INSTANT_DELIVERED ||
                            newStatus != OrderProductStatus.INSTANT_DELIVERED) {
                        throw new IllegalArgumentException(
                                "This inventory product is already instantly delivered."
                        );
                    }

                    return;
                }

                if (oldStatus == OrderProductStatus.PENDING_FOR_ALTERATION &&
                        newStatus == OrderProductStatus.CANCELLED) {

                    orderInventoryService.reverseInventoryItem(
                            order,
                            orderProduct
                    );

                    orderProduct.setStatus(newStatus);

                } else if (oldStatus == OrderProductStatus.PENDING_FOR_ALTERATION &&
                    newStatus == OrderProductStatus.ALTERATION_IN_PROGRESS) {

                orderInventoryService.consumeInventoryItem(
                        order,
                        orderProduct,
                        currentUser
                );

                orderProduct.setStatus(newStatus);
            } else if (oldStatus == OrderProductStatus.ALTERATION_IN_PROGRESS &&
                        newStatus == OrderProductStatus.COMPLETED) {

                    orderProduct.setStatus(newStatus);

                } else if (oldStatus == newStatus) {

                    return;

                } else {
                    throw new IllegalArgumentException(
                            "Invalid inventory alteration status transition: " +
                                    oldStatus + " -> " + newStatus
                    );
                }
            }

            /*
             * =========================================================
             * TAILORING PRODUCT
             * =========================================================
             */
            else if (productType == OrderProductType.TAILORING) {

                if (oldStatus == OrderProductStatus.PENDING &&
                        newStatus == OrderProductStatus.IN_PROGRESS) {

                    orderProduct.setStatus(newStatus);

                } else if (oldStatus == OrderProductStatus.IN_PROGRESS &&
                        newStatus == OrderProductStatus.COMPLETED) {

                    orderProduct.setStatus(newStatus);

                } else if (oldStatus == OrderProductStatus.PENDING &&
                        newStatus == OrderProductStatus.CANCELLED) {

                    orderInventoryService.releaseOrderProductReservation(
                            orderProduct
                    );

                    orderProduct.setStatus(newStatus);

                } else if (oldStatus == newStatus) {

                    return;

                } else {
                    throw new IllegalArgumentException(
                            "Invalid tailoring status transition: " +
                                    oldStatus + " -> " + newStatus
                    );
                }
            }

            orderProductRepository.save(orderProduct);

            logger.info(
                    "Order product status updated successfully. orderId={}, orderProductId={}, type={}, oldStatus={}, newStatus={}",
                    order.getOrderId(),
                    orderProduct.getId(),
                    productType,
                    oldStatus,
                    newStatus
            );

        } catch (IllegalArgumentException e) {

            logger.warn(
                    "Invalid order product status update. orderProductId={}, message={}",
                    dto != null ? dto.getOrderProductId() : null,
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            logger.error(
                    "Error updating order product status. orderProductId={}",
                    dto != null ? dto.getOrderProductId() : null,
                    e
            );

            throw new RuntimeException(
                    "Error updating product status.",
                    e
            );
        }
    }

    // Get order by ID
    public Order findById(Long id) {
        try {
            Optional<Order> order = orderRepository.findByIdAndUser(id, authenticatedUserService.getCurrentUser());
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


    @Transactional
    public Order updateOrder(Long orderId, UpdateCustomerOrderDto dto) {
        try {
            User currentUser = authenticatedUserService.getCurrentUser();

            Order existingOrder = orderRepository.findByIdAndUser(orderId, currentUser)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            if (existingOrder.getStatus() != OrderStatus.PENDING) {
                logger.warn(
                        "Attempt to update non-pending order. Order ID: {}, Status: {}",
                        existingOrder.getId(),
                        existingOrder.getStatus()
                );
                throw new RuntimeException("Only pending orders can be edited.");
            }

            if (dto.getOrderProducts() == null || dto.getOrderProducts().isEmpty()) {
                throw new RuntimeException("At least one product must be selected.");
            }

            BigDecimal oldTotal = existingOrder.getTotalProductAmount() != null
                    ? existingOrder.getTotalProductAmount()
                    : BigDecimal.ZERO;

            BigDecimal oldAdvance = existingOrder.getAdvancePayment() != null
                    ? existingOrder.getAdvancePayment()
                    : BigDecimal.ZERO;

            /*
             * Release all active reservations of this pending order.
             * Physical stock is NOT changed.
             */
            orderInventoryService.releasePendingOrderReservations(existingOrder);

            existingOrder.setOrderDate(dto.getOrderDate());
            existingOrder.setDeliveryDate(dto.getDeliveryDate());
            existingOrder.setUser(currentUser);

            List<OrderProduct> existingOrderProducts =
                    new ArrayList<>(existingOrder.getOrderProducts());

            Map<String, List<OrderProduct>> existingProductMap = new HashMap<>();

            for (OrderProduct existingProduct : existingOrderProducts) {
                String key = buildOrderProductKey(
                        existingProduct.getOrderProductType(),
                        existingProduct.getProduct() != null
                                ? existingProduct.getProduct().getId()
                                : null,
                        existingProduct.getFabricSource(),
                        existingProduct.getInventoryItem() != null
                                ? existingProduct.getInventoryItem().getId()
                                : null
                );

                existingProductMap
                        .computeIfAbsent(key, k -> new ArrayList<>())
                        .add(existingProduct);
            }

            Set<OrderProduct> reusedExistingProducts = new HashSet<>();
            List<OrderProduct> updatedOrderProducts = new ArrayList<>();

            BigDecimal newTotal = BigDecimal.ZERO;

            for (OrderProductDto productDto : dto.getOrderProducts()) {

                OrderProductType orderProductType =
                        productDto.getOrderProductType();

                if (orderProductType == null) {
                    orderProductType = OrderProductType.TAILORING;
                }

                /*
                 * =========================================================
                 * INVENTORY PRODUCT
                 * =========================================================
                 */
                if (orderProductType == OrderProductType.INVENTORY) {

                    if (productDto.getInventoryItemId() == null) {
                        throw new RuntimeException(
                                "Inventory item is required."
                        );
                    }

                    if (productDto.getQuantity() == null ||
                            productDto.getQuantity() <= 0) {
                        throw new RuntimeException(
                                "Inventory quantity must be greater than zero."
                        );
                    }

                    InventoryItem inventoryItem =
                            inventoryItemService.getById(
                                    productDto.getInventoryItemId()
                            );

                    if (inventoryItem == null) {
                        throw new RuntimeException(
                                "Inventory item not found."
                        );
                    }

                    if (Boolean.FALSE.equals(inventoryItem.getActive())) {
                        throw new RuntimeException(
                                "Selected inventory item is inactive: " +
                                        inventoryItem.getName()
                        );
                    }

                    String productKey = buildOrderProductKey(
                            OrderProductType.INVENTORY,
                            null,
                            null,
                            inventoryItem.getId()
                    );

                    OrderProduct orderProduct = null;

                    List<OrderProduct> matchingProducts =
                            existingProductMap.get(productKey);

                    if (matchingProducts != null &&
                            !matchingProducts.isEmpty()) {
                        orderProduct = matchingProducts.remove(0);
                        reusedExistingProducts.add(orderProduct);
                    }

                    if (orderProduct == null) {
                        orderProduct = new OrderProduct();
                        orderProduct.setOrder(existingOrder);
                    }

                    BigDecimal quantity =
                            BigDecimal.valueOf(productDto.getQuantity());

                    BigDecimal inventoryAmount =
                            orderInventoryService.reserveInventory(
                                    existingOrder,
                                    orderProduct,
                                    inventoryItem.getId(),
                                    quantity
                            );

                    BigDecimal inventoryUnitPrice =
                            quantity.compareTo(BigDecimal.ZERO) > 0
                                    ? inventoryAmount.divide(
                                    quantity,
                                    2,
                                    RoundingMode.HALF_UP
                            )
                                    : BigDecimal.ZERO;

                    orderProduct.setProduct(null);
                    orderProduct.setInventoryItem(inventoryItem);
                    orderProduct.setOrderProductType(
                            OrderProductType.INVENTORY
                    );
                    orderProduct.setQuantity(productDto.getQuantity());
                    orderProduct.setSilaiType(null);
                    orderProduct.setSilaiAmount(null);
                    orderProduct.setAdditionalNotes(
                            productDto.getAdditionalNotes()
                    );
                    orderProduct.setFabricSource(
                            FabricSource.CUSTOMER
                    );
                    orderProduct.setFabricQuantity(null);
                    orderProduct.setFabricUnitPrice(null);
                    orderProduct.setFabricAmount(null);
                    orderProduct.setInventoryUnitPrice(
                            inventoryUnitPrice
                    );
                    orderProduct.setInventoryAmount(
                            inventoryAmount
                    );
                    orderProduct.setSubtotal(inventoryAmount);

                    updatedOrderProducts.add(orderProduct);
                    newTotal = newTotal.add(inventoryAmount);

                    continue;
                }

                /*
                 * =========================================================
                 * TAILORING PRODUCT
                 * =========================================================
                 */

                if (productDto.getProductId() == null) {
                    throw new RuntimeException("Product is required.");
                }

                if (productDto.getQuantity() == null ||
                        productDto.getQuantity() <= 0) {
                    throw new RuntimeException(
                            "Product quantity must be greater than zero."
                    );
                }

                Product product = productRepository
                        .findByIdAndUser(
                                productDto.getProductId(),
                                currentUser
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found: " +
                                                productDto.getProductId()
                                )
                        );

                String silaiType = productDto.getSilaiType();

                if (silaiType == null || silaiType.isBlank()) {
                    throw new RuntimeException("Silai type is required.");
                }

                BigDecimal silaiAmount;

                if ("SINGLE".equalsIgnoreCase(silaiType)) {
                    silaiAmount = product.getSingleSilai();
                } else if ("DOUBLE".equalsIgnoreCase(silaiType)) {
                    silaiAmount = product.getDoubleSilai();
                } else {
                    throw new RuntimeException(
                            "Invalid silai type: " + silaiType
                    );
                }

                if (silaiAmount == null ||
                        silaiAmount.compareTo(BigDecimal.ZERO) < 0) {
                    throw new RuntimeException(
                            "Invalid silai amount for product: " +
                                    product.getName()
                    );
                }

                FabricSource fabricSource =
                        productDto.getFabricSource();

                if (fabricSource == null) {
                    fabricSource = FabricSource.CUSTOMER;
                }

                Long inventoryItemId = null;

                if (fabricSource == FabricSource.SHOP) {
                    inventoryItemId = productDto.getInventoryItemId();
                }

                String productKey = buildOrderProductKey(
                        OrderProductType.TAILORING,
                        product.getId(),
                        fabricSource,
                        inventoryItemId
                );

                OrderProduct orderProduct = null;

                List<OrderProduct> matchingProducts =
                        existingProductMap.get(productKey);

                if (matchingProducts != null &&
                        !matchingProducts.isEmpty()) {

                    orderProduct = matchingProducts.remove(0);
                    reusedExistingProducts.add(orderProduct);
                }

                if (orderProduct == null) {
                    orderProduct = new OrderProduct();
                    orderProduct.setOrder(existingOrder);
                }

                orderProduct.setOrderProductType(
                        OrderProductType.TAILORING
                );
                orderProduct.setProduct(product);
                orderProduct.setQuantity(productDto.getQuantity());
                orderProduct.setSilaiType(silaiType);
                orderProduct.setSilaiAmount(silaiAmount);
                orderProduct.setAdditionalNotes(
                        productDto.getAdditionalNotes()
                );
                orderProduct.setFabricSource(fabricSource);
                orderProduct.setInventoryUnitPrice(null);
                orderProduct.setInventoryAmount(null);

                BigDecimal stitchingAmount =
                        silaiAmount.multiply(
                                BigDecimal.valueOf(
                                        productDto.getQuantity()
                                )
                        );

                BigDecimal fabricAmount = BigDecimal.ZERO;

                /*
                 * =========================================================
                 * SHOP FABRIC
                 * =========================================================
                 */
                if (fabricSource == FabricSource.SHOP) {

                    if (productDto.getInventoryItemId() == null) {
                        throw new RuntimeException(
                                "Please select shop fabric for product: " +
                                        product.getName()
                        );
                    }

                    BigDecimal requestedQuantity =
                            productDto.getFabricQuantity();

                    if (requestedQuantity == null ||
                            requestedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new RuntimeException(
                                "Fabric quantity must be greater than zero for product: " +
                                        product.getName()
                        );
                    }

                    InventoryItem inventoryItem =
                            inventoryItemService.getById(
                                    productDto.getInventoryItemId()
                            );

                    if (inventoryItem == null) {
                        throw new RuntimeException(
                                "Inventory item not found."
                        );
                    }

                    if (Boolean.FALSE.equals(inventoryItem.getActive())) {
                        throw new RuntimeException(
                                "Selected fabric is inactive: " +
                                        inventoryItem.getName()
                        );
                    }

                    if (inventoryItem.getItemType() != ItemType.FABRIC) {
                        throw new RuntimeException(
                                "Selected inventory item is not a fabric."
                        );
                    }

                    /*
                     * FIFO reservation also calculates the correct
                     * batch-wise selling amount.
                     */
                    fabricAmount =
                            orderInventoryService.reserveInventory(
                                    existingOrder,
                                    orderProduct,
                                    inventoryItem.getId(),
                                    requestedQuantity
                            );

                    BigDecimal fabricUnitPrice =
                            requestedQuantity.compareTo(BigDecimal.ZERO) > 0
                                    ? fabricAmount.divide(
                                    requestedQuantity,
                                    2,
                                    RoundingMode.HALF_UP
                            )
                                    : BigDecimal.ZERO;

                    orderProduct.setInventoryItem(inventoryItem);
                    orderProduct.setFabricQuantity(
                            requestedQuantity
                    );
                    orderProduct.setFabricUnitPrice(
                            fabricUnitPrice
                    );
                    orderProduct.setFabricAmount(
                            fabricAmount
                    );

                } else {

                    orderProduct.setInventoryItem(null);
                    orderProduct.setFabricQuantity(null);
                    orderProduct.setFabricUnitPrice(null);
                    orderProduct.setFabricAmount(null);
                }

                BigDecimal subtotal =
                        stitchingAmount.add(fabricAmount);

                orderProduct.setSubtotal(subtotal);

                updatedOrderProducts.add(orderProduct);
                newTotal = newTotal.add(subtotal);
            }

            /*
             * Delete old products that are no longer present.
             */
            for (OrderProduct existingProduct : existingOrderProducts) {
                if (!reusedExistingProducts.contains(existingProduct)) {
                    existingOrder.getOrderProducts().remove(existingProduct);
                    orderProductRepository.delete(existingProduct);
                }
            }

            existingOrder.getOrderProducts().clear();
            existingOrder.getOrderProducts().addAll(updatedOrderProducts);

            for (OrderProduct orderProduct : updatedOrderProducts) {
                orderProduct.setOrder(existingOrder);
            }

            BigDecimal newAdvance =
                    dto.getAdvancePayment() != null
                            ? dto.getAdvancePayment()
                            : BigDecimal.ZERO;

            if (newAdvance.compareTo(newTotal) > 0) {
                throw new RuntimeException(
                        "Advance payment cannot be greater than total amount."
                );
            }

            BigDecimal newDue =
                    newTotal.subtract(newAdvance);

            existingOrder.setTotalProductAmount(newTotal);
            existingOrder.setAdvancePayment(newAdvance);
            existingOrder.setDuePayment(newDue);
            existingOrder.setPaidAmount(newAdvance);
            existingOrder.setOutstandingDueAmount(newDue);

            Order savedOrder =
                    orderRepository.save(existingOrder);

            /*
             * =========================================================
             * CUSTOMER LEDGER - ORDER AMOUNT DIFFERENCE
             * =========================================================
             */
            if (newTotal.compareTo(oldTotal) > 0) {

                BigDecimal difference =
                        newTotal.subtract(oldTotal);

                CustomerPaymentLedger ledger =
                        new CustomerPaymentLedger();

                ledger.setCustomer(savedOrder.getCustomer());
                ledger.setOrder(savedOrder);
                ledger.setAmount(difference);
                ledger.setPaymentType(PaymentType.CREDIT);
                ledger.setPaymentDate(LocalDate.now());
                ledger.setRemarks(
                        "Order amount increased"
                );

                customerPaymentRepository.save(ledger);

            } else if (newTotal.compareTo(oldTotal) < 0) {

                BigDecimal difference =
                        oldTotal.subtract(newTotal);

                CustomerPaymentLedger ledger =
                        new CustomerPaymentLedger();

                ledger.setCustomer(savedOrder.getCustomer());
                ledger.setOrder(savedOrder);
                ledger.setAmount(difference);
                ledger.setPaymentType(PaymentType.DEBIT);
                ledger.setPaymentDate(LocalDate.now());
                ledger.setRemarks(
                        "Order amount decreased"
                );

                customerPaymentRepository.save(ledger);
            }

            /*
             * =========================================================
             * CUSTOMER LEDGER - ADVANCE DIFFERENCE
             * =========================================================
             */
            if (newAdvance.compareTo(oldAdvance) > 0) {

                BigDecimal difference =
                        newAdvance.subtract(oldAdvance);

                CustomerPaymentLedger ledger =
                        new CustomerPaymentLedger();

                ledger.setCustomer(savedOrder.getCustomer());
                ledger.setOrder(savedOrder);
                ledger.setAmount(difference);
                ledger.setPaymentType(PaymentType.DEBIT);
                ledger.setPaymentDate(LocalDate.now());
                ledger.setRemarks(
                        "Advance payment received"
                );

                customerPaymentRepository.save(ledger);

            } else if (newAdvance.compareTo(oldAdvance) < 0) {

                BigDecimal difference =
                        oldAdvance.subtract(newAdvance);

                CustomerPaymentLedger ledger =
                        new CustomerPaymentLedger();

                ledger.setCustomer(savedOrder.getCustomer());
                ledger.setOrder(savedOrder);
                ledger.setAmount(difference);
                ledger.setPaymentType(PaymentType.CREDIT);
                ledger.setPaymentDate(LocalDate.now());
                ledger.setRemarks(
                        "Advance payment reduced"
                );

                customerPaymentRepository.save(ledger);
            }

            logger.info(
                    "Order updated successfully with FIFO reservations. " +
                            "Order ID: {}, Total: {}, Advance: {}, Due: {}",
                    savedOrder.getId(),
                    savedOrder.getTotalProductAmount(),
                    savedOrder.getAdvancePayment(),
                    savedOrder.getDuePayment()
            );

            return savedOrder;

        } catch (EntityNotFoundException e) {

            logger.error(
                    "Entity not found while updating order ID {}: {}",
                    orderId,
                    e.getMessage(),
                    e
            );

            throw new RuntimeException(
                    "Required data not found.",
                    e
            );

        } catch (Exception e) {

            logger.error(
                    "Error updating order ID {}: {}",
                    orderId,
                    e.getMessage(),
                    e
            );

            throw new RuntimeException(
                    e.getMessage(),
                    e
            );
        }
    }

    private String buildOrderProductKey(Long productId, FabricSource fabricSource, Long inventoryItemId) {
        return String.valueOf(productId) + "|" + String.valueOf(fabricSource) + "|" + String.valueOf(inventoryItemId);
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

            Pageable pageable = PageRequest.of(
                    0,
                    500,
                    Sort.by(
                            Sort.Order.desc("orderDate"),
                            Sort.Order.desc("id")
                    )
            ); // fetch more to allow in-memory pagination

            User currentUser = authenticatedUserService.getCurrentUser();
            Page<Order> orderPage = orderRepository.findBySearchAndDateRangeAndStatus(
                    (searchValue != null && !searchValue.isEmpty()) ? searchValue : null,
                    startDate,
                    endDate,
                    orderStatus,
                    currentUser,
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

            long countOrders = orderRepository.countByUser(currentUser);
            Map<String, Object> response = new HashMap<>();
            response.put("draw", draw);
            response.put("recordsTotal", countOrders);
            response.put("recordsFiltered", filtered.size());
            response.put("data", orderList);

            logger.info("Fetched paginated orders with custom payment filter - draw: {}, start: {}, length: {}, filtered: {}, total: {}",
                    draw, start, length, filtered.size(), countOrders);

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
        BigDecimal outstandingDueAmount = order.getOutstandingDueAmount();
        BigDecimal paid = order.getPaidAmount();
        PickupStatus pickupStatus = order.getPickupStatus();

        if (total == null || paid == null) return false;

        return switch (paymentStatus.toLowerCase()) {
            case "paid" -> paid.compareTo(total) == 0;
            case "unpaid" -> outstandingDueAmount.compareTo(total) == 0;
            case "partial_paid" -> pickupStatus == PickupStatus.PICKED_UP && paid.compareTo(total) < 0
                    && (order.getStatus().equals(OrderStatus.COMPLETED) ||
                    order.getStatus().equals(OrderStatus.DELIVERED));
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
        orderMap.put("customerName", order.getCustomer().getFullName());
        orderMap.put("customerPhone", order.getCustomer().getPhoneNumber());
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
        orderMap.put("outstandingDueAmount", order.getOutstandingDueAmount());

        return orderMap;
    }


    public List<Map<String, Object>> getOrderProducts(Order order) {
        List<OrderProduct> orderProducts = order.getOrderProducts();
        return orderProducts.stream().map(op -> {
            Map<String, Object> opMap = new HashMap<>();
            OrderProductType type = op.getOrderProductType();
            opMap.put("orderProductType", type != null ? type.name() : null);
            String unit = "";
            if (op.getInventoryItem() != null) {
                unit = op.getInventoryItem().getUnit().name();
            }

            opMap.put("quantity", op.getQuantity());
            opMap.put("unit", unit);
            opMap.put("subtotal", op.getSubtotal());
            if (type == OrderProductType.INVENTORY) {
                String productName = op.getInventoryItem() != null ? op.getInventoryItem().getName() : "Unknown Inventory Item";
                opMap.put("productName", productName);
                opMap.put("silaiType", null);
                opMap.put("amount", op.getInventoryAmount());
                opMap.put("inventoryItemName", productName);
                opMap.put("inventoryUnitPrice", op.getInventoryUnitPrice());
                opMap.put("inventoryAmount", op.getInventoryAmount());
                opMap.put("fabricSource", null);
                opMap.put("fabricQuantity", null);
                opMap.put("fabricUnitPrice", null);
                opMap.put("fabricAmount", null);
                opMap.put("alterationRequired", op.isAlterationRequired());
                opMap.put("alterationFee", op.getAlterationFee());
            } else if (type == OrderProductType.TAILORING) {
                String productName = op.getProduct() != null ? op.getProduct().getName() : "Unknown Product";
                opMap.put("productName", productName);
                opMap.put("silaiType", op.getSilaiType());
                opMap.put("amount", op.getSilaiAmount());
                opMap.put("inventoryItemName", op.getInventoryItem() != null ? op.getInventoryItem().getName() : null);
                opMap.put("inventoryUnitPrice", op.getInventoryUnitPrice());
                opMap.put("inventoryAmount", op.getInventoryAmount());
                opMap.put("fabricSource", op.getFabricSource() != null ? op.getFabricSource().name() : null);
                opMap.put("fabricQuantity", op.getFabricQuantity());
                opMap.put("fabricUnitPrice", op.getFabricUnitPrice());
                opMap.put("fabricAmount", op.getFabricAmount());
            } else {
                opMap.put("productName", "Unknown Product");
                opMap.put("silaiType", op.getSilaiType());
                opMap.put("amount", op.getSilaiAmount());
            }
            return opMap;
        }).toList();
    }

    @Transactional
    public void markOrderAsPickedUp(Order order, BigDecimal amountReceived) {
        logger.info("Attempting to mark order {} as picked up with amount received: {}", order.getOrderId(), amountReceived);

        try {
            if (order.getStatus() != OrderStatus.COMPLETED) {
                throw new IllegalArgumentException(
                        "Only completed orders can be picked up."
                );
            }

            if (order.getPickupStatus() == PickupStatus.PICKED_UP) {
                throw new IllegalArgumentException(
                        "Order has already been picked up."
                );
            }

            if (amountReceived == null) {
                amountReceived = BigDecimal.ZERO;
            }

            if (amountReceived.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Amount received cannot be negative."
                );
            }

            BigDecimal dueAmount = order.getDuePayment() != null
                    ? order.getDuePayment()
                    : BigDecimal.ZERO;

            BigDecimal advancePayment = order.getAdvancePayment() != null
                    ? order.getAdvancePayment()
                    : BigDecimal.ZERO;

            if (amountReceived.compareTo(dueAmount) > 0) {
                throw new IllegalArgumentException(
                        "Amount received cannot be greater than due."
                );
            }

            BigDecimal outstandingDueAmount =
                    dueAmount.subtract(amountReceived).max(BigDecimal.ZERO);

            order.setDuePayment(outstandingDueAmount);
            order.setPickupStatus(PickupStatus.PICKED_UP);
            order.setPickedUpWithDue(
                    outstandingDueAmount.compareTo(BigDecimal.ZERO) > 0
            );
            order.setPickupDate(LocalDateTime.now());

            order.setStatus(OrderStatus.DELIVERED);
            order.setOutstandingDueAmount(outstandingDueAmount);
            order.setPaidAmount(
                    advancePayment.add(amountReceived)
            );

            order.setUser(authenticatedUserService.getCurrentUser());
            orderRepository.save(order);

            CustomerPaymentLedger payment = new CustomerPaymentLedger();
            payment.setCustomer(order.getCustomer());
            payment.setOrder(order);
            payment.setPaymentType(PaymentType.DEBIT);

            if (amountReceived.compareTo(dueAmount) == 0) {
                payment.setRemarks("Full payment received at pickup");
            } else if (amountReceived.compareTo(BigDecimal.ZERO) == 0) {
                payment.setRemarks("No payment received at pickup");
            } else {
                payment.setRemarks("Partial payment received at pickup");
            }

            payment.setAmount(amountReceived);
            payment.setPaymentDate(LocalDate.now());
            customerPaymentRepository.save(payment);

            logger.info(
                    "Order {} marked as picked up. Amount received={}, outstanding due={}",
                    order.getOrderId(),
                    amountReceived,
                    outstandingDueAmount
            );

        } catch (IllegalArgumentException e) {
            logger.warn(
                    "Invalid input for marking order {} as picked up: {}",
                    order.getOrderId(),
                    e.getMessage()
            );
            throw e;
        } catch (Exception e) {
            logger.error(
                    "Error marking order {} as picked up: {}",
                    order.getOrderId(),
                    e.getMessage(),
                    e
            );
            throw new RuntimeException(
                    "Error marking order as picked up.",
                    e
            );
        }
    }

    public List<Order> getOrdersWithOutstandingDue() {
        try {
            List<Order> orders = orderRepository.findOrdersWithOutstandingDue(authenticatedUserService.getCurrentUser());
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

            if (order.getOutstandingDueAmount().compareTo(paymentAmount) == 0) {
                order.setDuePayment(BigDecimal.ZERO);
            }

            // Process payment
            order.setPaidAmount(order.getPaidAmount().add(paymentAmount));
            order.setOutstandingDueAmount(outstandingDue.subtract(paymentAmount));

//            if (order.getOutstandingDueAmount().compareTo(BigDecimal.ZERO) == 0) {
//                order.setStatus(OrderStatus.DELIVERED);
//            }

            order.setUser(authenticatedUserService.getCurrentUser());
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
            long count = orderRepository.countByUser(authenticatedUserService.getCurrentUser());
            logger.debug("Total orders count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting total orders count: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting total orders count.", e);
        }
    }

    public Long getPendingPaymentsCount() {
        try {
            long count = orderRepository.countPendingPayments(authenticatedUserService.getCurrentUser());
            logger.debug("Pending payments count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting pending payments count: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting pending payments count.", e);
        }
    }

    public BigDecimal getPendingPaymentsAmount() {
        return orderRepository.sumPendingPaymentsAmount((authenticatedUserService.getCurrentUser()));
    }

    public Long getOrdersInProgressCount() {
        try {
            long count = orderRepository.countOrdersInProgress(authenticatedUserService.getCurrentUser());
            logger.debug("Orders in progress count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting orders in progress count: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting orders in progress count.", e);
        }
    }

    public Long getCompletedOrdersCount() {
        try {
            long count = orderRepository.countCompletedOrders(authenticatedUserService.getCurrentUser());
            logger.debug("Completed orders count: {}", count);
            return count;
        } catch (Exception e) {
            logger.error("Error getting completed orders count: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting completed orders count.", e);
        }
    }

    public Map<String, Long> getOrderStatusCounts() {
        try {
            Map<String, Long> statusCounts = new LinkedHashMap<>();
            statusCounts.put("Cancelled", orderRepository.countCancelledOrders(authenticatedUserService.getCurrentUser()));
            statusCounts.put("Pending", orderRepository.countPendingOrders(authenticatedUserService.getCurrentUser()));
            statusCounts.put("InProgress", orderRepository.countOrdersInProgress(authenticatedUserService.getCurrentUser()));
            statusCounts.put("Completed", orderRepository.countCompletedOrders(authenticatedUserService.getCurrentUser()));
            statusCounts.put("Delivered", orderRepository.countDeliveredOrders(authenticatedUserService.getCurrentUser()));
            logger.debug("Fetched order status counts: {}", statusCounts);
            return statusCounts;
        } catch (Exception e) {
            logger.error("Error fetching order status counts: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching order status counts.", e);
        }
    }

    public List<Order> getTop5ByOrderByCreatedAtDesc() {
        try {
            List<Order> top5Orders = orderRepository.findTop5ByUserOrderByCreatedAtDesc(authenticatedUserService.getCurrentUser());
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
            List<Object[]> results = orderRepository.findTopCustomersWithDetails(authenticatedUserService.getCurrentUser(), topFive);

            List<Map<String, Object>> topCustomers = results.stream().map(obj -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", new ThymeleafUtil().encryptId((Long) obj[0]));
                map.put("name", obj[1]);
                map.put("phone", obj[2]);
                map.put("totalOrders", obj[3]);
                map.put("totalPaid", obj[4]);
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
            List<OrderStatus> excludedStatuses = Arrays.asList(OrderStatus.CANCELLED, OrderStatus.COMPLETED, OrderStatus.DELIVERED);

            // Fetch orders that are either upcoming or overdue but still not completed
            List<Order> upcomingOrOverdueOrders = orderRepository
                    .findByUserAndDeliveryDateLessThanEqualAndStatusNotIn(authenticatedUserService.getCurrentUser(), threeDaysLater, excludedStatuses);

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

    private String buildOrderProductKey(
            OrderProductType orderProductType,
            Long productId,
            FabricSource fabricSource,
            Long inventoryItemId
    ) {
        return String.valueOf(orderProductType) +
                "|" +
                String.valueOf(productId) +
                "|" +
                String.valueOf(fabricSource) +
                "|" +
                String.valueOf(inventoryItemId);
    }

    public List<Order> getUpcomingDeliveries(LocalDate currentDate, int limit) {

        List<OrderStatus> excludedStatuses = Arrays.asList(
                OrderStatus.CANCELLED,
                OrderStatus.DELIVERED
        );

        return orderRepository.findUpcomingDeliveries(
                authenticatedUserService.getCurrentUser(),
                currentDate,
                excludedStatuses,
                PageRequest.of(0, limit)
        );
    }
}
