package com.example.tailormaster.service;

import com.example.tailormaster.dto.OrderInventoryPreviewDto;
import com.example.tailormaster.entity.*;
import com.example.tailormaster.enums.FabricSource;
import com.example.tailormaster.enums.OrderProductType;
import com.example.tailormaster.enums.StockMovementType;
import com.example.tailormaster.enums.StockReferenceType;
import com.example.tailormaster.repository.*;
import com.example.tailormaster.util.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class OrderInventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryStockBatchRepository inventoryStockBatchRepository;
    private final OrderInventoryUsageRepository orderInventoryUsageRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final OrderInventoryReservationRepository orderInventoryReservationRepository;

    public void consumeShopFabric(Order order) {
        if (order == null || order.getOrderProducts() == null) {
            return;
        }

        User currentUser = authenticatedUserService.getCurrentUser();

        log.info(
                "Starting inventory consumption. orderId={}, orderPkId={}, products={}",
                order.getOrderId(),
                order.getId(),
                order.getOrderProducts().size()
        );

        for (OrderProduct orderProduct : order.getOrderProducts()) {

            OrderProductType orderProductType = orderProduct.getOrderProductType();

            log.info(
                    "Checking order product. orderProductId={}, type={}, product={}, fabricSource={}, inventoryItemId={}, fabricQuantity={}, quantity={}",
                    orderProduct.getId(),
                    orderProductType,
                    orderProduct.getProduct() != null
                            ? orderProduct.getProduct().getName()
                            : null,
                    orderProduct.getFabricSource(),
                    orderProduct.getInventoryItem() != null
                            ? orderProduct.getInventoryItem().getId()
                            : null,
                    orderProduct.getFabricQuantity(),
                    orderProduct.getQuantity()
            );

            if (orderProductType == OrderProductType.INVENTORY) {
                consumeInventoryItem(order, orderProduct, currentUser);
                continue;
            }

            if (orderProductType != OrderProductType.TAILORING) {
                continue;
            }

            if (orderProduct.getFabricSource() != FabricSource.SHOP) {
                continue;
            }

            consumeShopFabricForTailoring(order, orderProduct, currentUser);
        }

        log.info(
                "Finished inventory consumption for order {}",
                order.getOrderId()
        );
    }

    private void consumeInventoryItem(
            Order order,
            OrderProduct orderProduct,
            User currentUser
    ) {
        if (orderProduct.getInventoryItem() == null) {
            throw new IllegalStateException(
                    "Inventory item is missing for inventory order product."
            );
        }

        if (orderProduct.getQuantity() < 1) {
            throw new IllegalStateException(
                    "Invalid quantity for inventory order product."
            );
        }

        if (orderInventoryUsageRepository.existsByUserAndOrderProductId(
                currentUser,
                orderProduct.getId()
        )) {
            log.info(
                    "Inventory already consumed for orderProductId={}",
                    orderProduct.getId()
            );
            return;
        }

        BigDecimal requiredQuantity =
                BigDecimal.valueOf(orderProduct.getQuantity());

        consumeFromBatches(
                order,
                orderProduct,
                orderProduct.getInventoryItem().getId(),
                requiredQuantity,
                currentUser,
                "Customer Order - Inventory Item"
        );
    }

    private void consumeShopFabricForTailoring(
            Order order,
            OrderProduct orderProduct,
            User currentUser
    ) {
        if (orderProduct.getInventoryItem() == null) {
            throw new IllegalStateException(
                    "Shop fabric inventory item is missing for order product."
            );
        }

        if (orderProduct.getFabricQuantity() == null ||
                orderProduct.getFabricQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "Invalid fabric quantity for order product."
            );
        }

        if (orderInventoryUsageRepository.existsByUserAndOrderProductId(
                currentUser,
                orderProduct.getId()
        )) {
            log.info(
                    "Shop fabric already consumed for orderProductId={}",
                    orderProduct.getId()
            );
            return;
        }

        consumeFromBatches(
                order,
                orderProduct,
                orderProduct.getInventoryItem().getId(),
                orderProduct.getFabricQuantity(),
                currentUser,
                "Customer Order - Shop Fabric"
        );
    }

    private void consumeFromBatches(
            Order order,
            OrderProduct orderProduct,
            Long inventoryItemId,
            BigDecimal requiredQuantity,
            User currentUser,
            String movementReason
    ) {
        InventoryItem inventoryItem =
                inventoryItemRepository.findByIdForUpdate(
                        inventoryItemId,
                        currentUser
                ).orElseThrow(() -> new IllegalStateException(
                        "Inventory item not found: " + inventoryItemId
                ));

        if (Boolean.FALSE.equals(inventoryItem.getActive())) {
            throw new IllegalStateException(
                    "Inventory item is inactive: " + inventoryItem.getName()
            );
        }

        List<OrderInventoryReservation> reservations =
                orderInventoryReservationRepository
                        .findByUserAndOrderProductIdAndReleasedFalse(
                                currentUser,
                                orderProduct.getId()
                        );

        if (reservations.isEmpty()) {
            throw new IllegalStateException(
                    "No active inventory reservation found for order product: " +
                            orderProduct.getId()
            );
        }

        BigDecimal reservedTotal = reservations.stream()
                .map(OrderInventoryReservation::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (requiredQuantity.compareTo(reservedTotal) != 0) {
            throw new IllegalStateException(
                    "Inventory reservation mismatch for order product: " +
                            orderProduct.getId() +
                            ". Required: " +
                            requiredQuantity.stripTrailingZeros().toPlainString() +
                            ", Reserved: " +
                            reservedTotal.stripTrailingZeros().toPlainString()
            );
        }

        BigDecimal remainingToConsume = requiredQuantity;

        for (OrderInventoryReservation reservation : reservations) {

            if (remainingToConsume.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            InventoryStockBatch batch = reservation.getStockBatch();

            if (batch == null) {
                throw new IllegalStateException(
                        "Stock batch is missing for reservation: " +
                                reservation.getId()
                );
            }

            BigDecimal reservationQuantity =
                    reservation.getQuantity() != null
                            ? reservation.getQuantity()
                            : BigDecimal.ZERO;

            if (reservationQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal batchRemaining =
                    batch.getRemainingQuantity() != null
                            ? batch.getRemainingQuantity()
                            : BigDecimal.ZERO;

            if (reservationQuantity.compareTo(batchRemaining) > 0) {
                throw new IllegalStateException(
                        "Reserved quantity is greater than available batch stock. " +
                                "Batch ID: " + batch.getId() +
                                ", Reserved: " + reservationQuantity +
                                ", Available: " + batchRemaining
                );
            }

            BigDecimal consumedQuantity =
                    remainingToConsume.min(reservationQuantity);

            batch.setRemainingQuantity(
                    batchRemaining.subtract(consumedQuantity)
            );

            if (batch.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) {
                batch.setActive(false);
            }

            inventoryStockBatchRepository.save(batch);

            OrderInventoryUsage usage = new OrderInventoryUsage();
            usage.setOrder(order);
            usage.setOrderProduct(orderProduct);
            usage.setUser(currentUser);
            usage.setInventoryItem(inventoryItem);
            usage.setStockBatch(batch);
            usage.setQuantity(consumedQuantity);
            usage.setUnit(
                    inventoryItem.getUnit() != null
                            ? inventoryItem.getUnit().name()
                            : null
            );
            usage.setUnitPrice(reservation.getUnitPrice());
            usage.setReversed(false);

            orderInventoryUsageRepository.save(usage);

            StockMovement movement = new StockMovement();
            movement.setInventoryItem(inventoryItem);
            movement.setUser(currentUser);
            movement.setMovementType(StockMovementType.OUT);
            movement.setQuantity(consumedQuantity);
            movement.setReferenceType(StockReferenceType.ORDER);
            movement.setReferenceId(order.getId());
            movement.setReason(movementReason);
            movement.setNotes(
                    "Order ID: " + order.getOrderId() +
                            ", Batch ID: " + batch.getId() +
                            ", Unit Price: " + reservation.getUnitPrice()
            );

            stockMovementRepository.save(movement);

            reservation.setReleased(true);
            reservation.setReleasedAt(LocalDateTime.now());
            orderInventoryReservationRepository.save(reservation);

            remainingToConsume =
                    remainingToConsume.subtract(consumedQuantity);

            log.info(
                    "Reserved batch consumed. orderId={}, orderProductId={}, inventoryItemId={}, batchId={}, quantity={}, unitPrice={}, batchRemaining={}",
                    order.getOrderId(),
                    orderProduct.getId(),
                    inventoryItem.getId(),
                    batch.getId(),
                    consumedQuantity,
                    reservation.getUnitPrice(),
                    batch.getRemainingQuantity()
            );
        }

        if (remainingToConsume.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException(
                    "Unable to consume complete reserved quantity for inventory item: " +
                            inventoryItem.getName()
            );
        }

        BigDecimal currentStock = inventoryItem.getQuantity() != null
                ? inventoryItem.getQuantity()
                : BigDecimal.ZERO;

        if (requiredQuantity.compareTo(currentStock) > 0) {
            throw new IllegalStateException(
                    "Inventory stock became inconsistent for item: " +
                            inventoryItem.getName()
            );
        }

        inventoryItem.setQuantity(
                currentStock.subtract(requiredQuantity)
        );

        inventoryItemRepository.save(inventoryItem);

        log.info(
                "Reserved inventory consumption completed. orderId={}, orderProductId={}, inventoryItemId={}, quantity={}, remainingStock={}",
                order.getOrderId(),
                orderProduct.getId(),
                inventoryItem.getId(),
                requiredQuantity,
                inventoryItem.getQuantity()
        );
    }

    public void reverseShopFabric(Order order) {
        if (order == null) {
            return;
        }

        User currentUser = authenticatedUserService.getCurrentUser();

        List<OrderInventoryReservation> reservations =
                orderInventoryReservationRepository
                        .findByUserAndOrderIdAndReleasedFalse(
                                currentUser,
                                order.getId()
                        );

        for (OrderInventoryReservation reservation : reservations) {
            reservation.setReleased(true);
            reservation.setReleasedAt(LocalDateTime.now());
            orderInventoryReservationRepository.save(reservation);

            log.info(
                    "Inventory reservation released. orderId={}, orderProductId={}, inventoryItemId={}, batchId={}, quantity={}",
                    order.getOrderId(),
                    reservation.getOrderProduct().getId(),
                    reservation.getInventoryItem().getId(),
                    reservation.getStockBatch().getId(),
                    reservation.getQuantity()
            );
        }

        List<OrderInventoryUsage> usages =
                orderInventoryUsageRepository
                        .findByUserAndOrderIdAndReversedFalse(
                                currentUser,
                                order.getId()
                        );

        for (OrderInventoryUsage usage : usages) {

            InventoryItem inventoryItem =
                    inventoryItemRepository.findByIdForUpdate(
                            usage.getInventoryItem().getId(),
                            currentUser
                    ).orElseThrow(() -> new IllegalStateException(
                            "Inventory item not found: " +
                                    usage.getInventoryItem().getId()
                    ));

            BigDecimal currentStock = inventoryItem.getQuantity() != null
                    ? inventoryItem.getQuantity()
                    : BigDecimal.ZERO;

            BigDecimal restoredStock =
                    currentStock.add(usage.getQuantity());

            inventoryItem.setQuantity(restoredStock);
            inventoryItemRepository.save(inventoryItem);

            InventoryStockBatch batch = usage.getStockBatch();

            if (batch != null) {
                BigDecimal batchRemaining =
                        batch.getRemainingQuantity() != null
                                ? batch.getRemainingQuantity()
                                : BigDecimal.ZERO;

                batch.setRemainingQuantity(
                        batchRemaining.add(usage.getQuantity())
                );

                inventoryStockBatchRepository.save(batch);

                log.info(
                        "FIFO batch restored. orderId={}, orderProductId={}, inventoryItemId={}, batchId={}, quantity={}, batchRemaining={}",
                        order.getOrderId(),
                        usage.getOrderProduct().getId(),
                        inventoryItem.getId(),
                        batch.getId(),
                        usage.getQuantity(),
                        batch.getRemainingQuantity()
                );
            } else {
                log.warn(
                        "Historical usage has no stock batch. usageId={}, orderProductId={}, inventoryItemId={}",
                        usage.getId(),
                        usage.getOrderProduct().getId(),
                        inventoryItem.getId()
                );
            }

            StockMovement movement = new StockMovement();
            movement.setInventoryItem(inventoryItem);
            movement.setUser(currentUser);
            movement.setMovementType(StockMovementType.IN);
            movement.setQuantity(usage.getQuantity());
            movement.setReferenceType(StockReferenceType.ORDER);
            movement.setReferenceId(order.getId());
            movement.setReason("Order Cancellation - Stock Reversal");
            movement.setNotes(
                    "Stock restored for cancelled Order ID: " +
                            order.getOrderId() +
                            (batch != null
                                    ? ", Batch ID: " + batch.getId()
                                    : "")
            );

            stockMovementRepository.save(movement);

            usage.setReversed(true);
            usage.setReversedAt(LocalDateTime.now());

            orderInventoryUsageRepository.save(usage);

            log.info(
                    "Inventory stock reversed. orderId={}, orderProductId={}, inventoryItemId={}, quantity={}, restoredStock={}",
                    order.getOrderId(),
                    usage.getOrderProduct().getId(),
                    inventoryItem.getId(),
                    usage.getQuantity(),
                    restoredStock
            );
        }
    }

    public BigDecimal reserveInventory(
            Order order,
            OrderProduct orderProduct,
            Long inventoryItemId,
            BigDecimal requiredQuantity
    ) {

        BigDecimal totalAmount = BigDecimal.ZERO;
        if (order == null || orderProduct == null) {
            throw new IllegalArgumentException("Order and order product are required");
        }

        if (inventoryItemId == null) {
            throw new IllegalArgumentException("Inventory item is required");
        }

        if (requiredQuantity == null ||
                requiredQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be greater than zero");
        }

        User currentUser = authenticatedUserService.getCurrentUser();

        if (orderInventoryReservationRepository
                .existsByUserAndOrderProductIdAndReleasedFalse(
                        currentUser,
                        orderProduct.getId()
                )) {
            log.info(
                    "Inventory already reserved. orderProductId={}",
                    orderProduct.getId()
            );
            return totalAmount;
        }

        InventoryItem inventoryItem =
                inventoryItemRepository.findByIdForUpdate(
                        inventoryItemId,
                        currentUser
                ).orElseThrow(() -> new IllegalStateException(
                        "Inventory item not found: " + inventoryItemId
                ));

        if (Boolean.FALSE.equals(inventoryItem.getActive())) {
            throw new IllegalStateException(
                    "Inventory item is inactive: " + inventoryItem.getName()
            );
        }

        List<InventoryStockBatch> batches =
                inventoryStockBatchRepository.findAvailableBatchesForUpdate(
                        currentUser,
                        inventoryItemId
                );

        List<OrderInventoryReservation> existingReservations =
                orderInventoryReservationRepository
                        .findByUserAndInventoryItemIdAndReleasedFalse(
                                currentUser,
                                inventoryItemId
                        );

        Map<Long, BigDecimal> reservedByBatch = new HashMap<>();

        for (OrderInventoryReservation reservation : existingReservations) {
            if (reservation.getStockBatch() == null) {
                continue;
            }

            Long batchId = reservation.getStockBatch().getId();

            BigDecimal reservedQuantity =
                    reservation.getQuantity() != null
                            ? reservation.getQuantity()
                            : BigDecimal.ZERO;

            reservedByBatch.merge(
                    batchId,
                    reservedQuantity,
                    BigDecimal::add
            );
        }

        BigDecimal totalAvailable = BigDecimal.ZERO;

        for (InventoryStockBatch batch : batches) {
            BigDecimal batchRemaining =
                    batch.getRemainingQuantity() != null
                            ? batch.getRemainingQuantity()
                            : BigDecimal.ZERO;

            BigDecimal alreadyReserved =
                    reservedByBatch.getOrDefault(
                            batch.getId(),
                            BigDecimal.ZERO
                    );

            BigDecimal batchAvailable =
                    batchRemaining.subtract(alreadyReserved);

            if (batchAvailable.compareTo(BigDecimal.ZERO) > 0) {
                totalAvailable = totalAvailable.add(batchAvailable);
            }
        }

        if (requiredQuantity.compareTo(totalAvailable) > 0) {
            throw new IllegalStateException(
                    "Insufficient available stock for inventory item: " +
                            inventoryItem.getName() +
                            ". Required: " +
                            requiredQuantity.stripTrailingZeros().toPlainString() +
                            ", Available: " +
                            totalAvailable.stripTrailingZeros().toPlainString()
            );
        }

        BigDecimal remainingToReserve = requiredQuantity;

        for (InventoryStockBatch batch : batches) {
            if (remainingToReserve.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal batchRemaining =
                    batch.getRemainingQuantity() != null
                            ? batch.getRemainingQuantity()
                            : BigDecimal.ZERO;

            BigDecimal alreadyReserved =
                    reservedByBatch.getOrDefault(
                            batch.getId(),
                            BigDecimal.ZERO
                    );

            BigDecimal batchAvailable =
                    batchRemaining.subtract(alreadyReserved);

            if (batchAvailable.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal reservedQuantity =
                    remainingToReserve.min(batchAvailable);

            OrderInventoryReservation reservation =
                    new OrderInventoryReservation();

            reservation.setOrder(order);
            reservation.setOrderProduct(orderProduct);
            reservation.setUser(currentUser);
            reservation.setInventoryItem(inventoryItem);
            reservation.setStockBatch(batch);
            reservation.setQuantity(reservedQuantity);
            reservation.setUnitPrice(batch.getSalePrice());
            reservation.setReleased(false);

            orderInventoryReservationRepository.save(reservation);

            BigDecimal reservedAmount =
                    reservedQuantity.multiply(batch.getSalePrice());

            totalAmount = totalAmount.add(reservedAmount);

            remainingToReserve =
                    remainingToReserve.subtract(reservedQuantity);

            log.info(
                    "FIFO inventory batch reserved. orderId={}, orderProductId={}, inventoryItemId={}, batchId={}, quantity={}, unitPrice={}",
                    order.getOrderId(),
                    orderProduct.getId(),
                    inventoryItem.getId(),
                    batch.getId(),
                    reservedQuantity,
                    batch.getSalePrice()
            );
        }

        if (remainingToReserve.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException(
                    "Unable to reserve complete quantity for inventory item: " +
                            inventoryItem.getName()
            );
        }

        return totalAmount;
    }

    @Transactional(readOnly = true)
    public OrderInventoryPreviewDto previewInventoryAmount(Long inventoryItemId, BigDecimal quantity) {
        User currentUser = authenticatedUserService.getCurrentUser();

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        InventoryItem inventoryItem =
                inventoryItemRepository.findByUserAndId(currentUser, inventoryItemId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("Inventory item not found."));

        if (Boolean.FALSE.equals(inventoryItem.getActive())) {
            throw new IllegalArgumentException("Inventory item is inactive.");
        }

        List<InventoryStockBatch> batches =
                inventoryStockBatchRepository.findByUserAndInventoryItemIdAndRemainingQuantityGreaterThanOrderByReceivedDateAscIdAsc(
                        currentUser,
                        inventoryItemId,
                        BigDecimal.ZERO
                );

        BigDecimal alreadyReserved =
                orderInventoryReservationRepository
                        .findByUserAndInventoryItemIdAndReleasedFalse(
                                currentUser,
                                inventoryItemId
                        )
                        .stream()
                        .map(OrderInventoryReservation::getQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableQuantity =
                inventoryItem.getQuantity().subtract(alreadyReserved);

        if (availableQuantity.compareTo(quantity) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient inventory stock. Available: " +
                            availableQuantity.stripTrailingZeros().toPlainString()
            );
        }

        BigDecimal remainingRequired = quantity;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (InventoryStockBatch batch : batches) {
            if (remainingRequired.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal batchAvailable = batch.getRemainingQuantity();

            BigDecimal reservedFromBatch =
                    orderInventoryReservationRepository
                            .findByUserAndInventoryItemIdAndReleasedFalse(
                                    currentUser,
                                    inventoryItemId
                            )
                            .stream()
                            .filter(r -> r.getStockBatch().getId().equals(batch.getId()))
                            .map(OrderInventoryReservation::getQuantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            batchAvailable = batchAvailable.subtract(reservedFromBatch);

            if (batchAvailable.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal consumeQuantity =
                    batchAvailable.min(remainingRequired);

            totalAmount = totalAmount.add(
                    consumeQuantity.multiply(batch.getSalePrice())
            );

            remainingRequired = remainingRequired.subtract(consumeQuantity);
        }

        if (remainingRequired.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException(
                    "Unable to calculate FIFO inventory price."
            );
        }

        BigDecimal averageUnitPrice =
                totalAmount.divide(
                        quantity,
                        2,
                        RoundingMode.HALF_UP
                );

        return new OrderInventoryPreviewDto(
                quantity,
                totalAmount,
                averageUnitPrice
        );
    }

    public BigDecimal getReservedQuantity(Long inventoryItemId) {
        User currentUser = authenticatedUserService.getCurrentUser();

        BigDecimal reservedQuantity =
                orderInventoryReservationRepository.getReservedQuantity(
                        currentUser,
                        inventoryItemId
                );

        return reservedQuantity != null
                ? reservedQuantity
                : BigDecimal.ZERO;
    }

    public void releasePendingOrderReservations(Order order) {
        if (order == null) {
            return;
        }

        User currentUser = authenticatedUserService.getCurrentUser();

        List<OrderInventoryReservation> reservations =
                orderInventoryReservationRepository
                        .findByUserAndOrderIdAndReleasedFalse(
                                currentUser,
                                order.getId()
                        );

        for (OrderInventoryReservation reservation : reservations) {

            reservation.setReleased(true);
            reservation.setReleasedAt(LocalDateTime.now());

            orderInventoryReservationRepository.save(reservation);

            log.info(
                    "Pending order inventory reservation released during edit. " +
                            "orderId={}, orderProductId={}, inventoryItemId={}, " +
                            "batchId={}, quantity={}",
                    order.getOrderId(),
                    reservation.getOrderProduct().getId(),
                    reservation.getInventoryItem().getId(),
                    reservation.getStockBatch().getId(),
                    reservation.getQuantity()
            );
        }
    }
}