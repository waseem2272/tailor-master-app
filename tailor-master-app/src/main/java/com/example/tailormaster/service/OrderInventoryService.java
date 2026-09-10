package com.example.tailormaster.service;

import com.example.tailormaster.entity.InventoryItem;
import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.OrderInventoryUsage;
import com.example.tailormaster.entity.OrderProduct;
import com.example.tailormaster.entity.StockMovement;
import com.example.tailormaster.enums.FabricSource;
import com.example.tailormaster.enums.StockMovementType;
import com.example.tailormaster.enums.StockReferenceType;
import com.example.tailormaster.repository.InventoryItemRepository;
import com.example.tailormaster.repository.OrderInventoryUsageRepository;
import com.example.tailormaster.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class OrderInventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final OrderInventoryUsageRepository orderInventoryUsageRepository;
    private final StockMovementRepository stockMovementRepository;

    public void consumeShopFabric(Order order) {

        if (order == null || order.getOrderProducts() == null) {
            return;
        }

        log.info(
                "Starting shop fabric consumption. orderId={}, orderPkId={}, products={}",
                order.getOrderId(),
                order.getId(),
                order.getOrderProducts() != null
                        ? order.getOrderProducts().size()
                        : 0
        );

        for (OrderProduct orderProduct : order.getOrderProducts()) {

            log.info(
                    "Checking order product. orderProductId={}, product={}, fabricSource={}, inventoryItemId={}, fabricQuantity={}",
                    orderProduct.getId(),
                    orderProduct.getProduct() != null
                            ? orderProduct.getProduct().getName()
                            : null,
                    orderProduct.getFabricSource(),
                    orderProduct.getInventoryItem() != null
                            ? orderProduct.getInventoryItem().getId()
                            : null,
                    orderProduct.getFabricQuantity()
            );

            if (orderProduct.getFabricSource() != FabricSource.SHOP) {
                continue;
            }

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

            if (orderInventoryUsageRepository.existsByOrderProductId(orderProduct.getId())) {
                log.info(
                        "Stock already consumed for orderProductId={}",
                        orderProduct.getId()
                );
                continue;
            }

            Long inventoryItemId = orderProduct.getInventoryItem().getId();

            InventoryItem inventoryItem =
                    inventoryItemRepository.findByIdForUpdate(inventoryItemId)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Inventory item not found: " + inventoryItemId
                            ));

            BigDecimal requiredQuantity = orderProduct.getFabricQuantity();

            BigDecimal currentStock = inventoryItem.getQuantity() != null
                    ? inventoryItem.getQuantity()
                    : BigDecimal.ZERO;

            if (requiredQuantity.compareTo(currentStock) > 0) {
                throw new IllegalStateException(
                        "Insufficient stock for fabric: " +
                                inventoryItem.getName() +
                                ". Required: " +
                                requiredQuantity.stripTrailingZeros().toPlainString() +
                                ", Available: " +
                                currentStock.stripTrailingZeros().toPlainString()
                );
            }

            BigDecimal remainingStock =
                    currentStock.subtract(requiredQuantity);

            inventoryItem.setQuantity(remainingStock);
            inventoryItemRepository.save(inventoryItem);

            StockMovement movement = new StockMovement();
            movement.setInventoryItem(inventoryItem);
            movement.setMovementType(StockMovementType.OUT);
            movement.setQuantity(requiredQuantity);
            movement.setReferenceType(StockReferenceType.ORDER);
            movement.setReferenceId(order.getId());
            movement.setReason("Customer Order - Shop Fabric");
            movement.setNotes(
                    "Order ID: " + order.getOrderId()
            );

            stockMovementRepository.save(movement);

            OrderInventoryUsage usage = new OrderInventoryUsage();
            usage.setOrder(order);
            usage.setOrderProduct(orderProduct);
            usage.setInventoryItem(inventoryItem);
            usage.setQuantity(requiredQuantity);
            usage.setUnit(
                    inventoryItem.getUnit() != null
                            ? inventoryItem.getUnit().name()
                            : null
            );
            usage.setReversed(false);

            orderInventoryUsageRepository.save(usage);

            log.info(
                    "Shop fabric stock consumed. orderId={}, orderProductId={}, inventoryItemId={}, quantity={}, remainingStock={}",
                    order.getOrderId(),
                    orderProduct.getId(),
                    inventoryItem.getId(),
                    requiredQuantity,
                    remainingStock
            );
        }

        log.info(
                "Finished shop fabric consumption for order {}",
                order.getOrderId()
        );
    }

    public void reverseShopFabric(Order order) {

        List<OrderInventoryUsage> usages =
                orderInventoryUsageRepository
                        .findByOrderIdAndReversedFalse(order.getId());

        for (OrderInventoryUsage usage : usages) {

            InventoryItem inventoryItem =
                    inventoryItemRepository.findByIdForUpdate(
                                    usage.getInventoryItem().getId())
                            .orElseThrow(() -> new IllegalStateException(
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

            StockMovement movement = new StockMovement();
            movement.setInventoryItem(inventoryItem);
            movement.setMovementType(StockMovementType.IN);
            movement.setQuantity(usage.getQuantity());
            movement.setReferenceType(StockReferenceType.ORDER);
            movement.setReferenceId(order.getId());
            movement.setReason("Order Cancellation - Stock Reversal");
            movement.setNotes(
                    "Stock restored for cancelled Order ID: " +
                            order.getOrderId()
            );

            stockMovementRepository.save(movement);

            usage.setReversed(true);
            usage.setReversedAt(LocalDateTime.now());

            orderInventoryUsageRepository.save(usage);

            log.info(
                    "Shop fabric stock reversed. orderId={}, inventoryItemId={}, quantity={}, restoredStock={}",
                    order.getOrderId(),
                    inventoryItem.getId(),
                    usage.getQuantity(),
                    restoredStock
            );
        }
    }
}