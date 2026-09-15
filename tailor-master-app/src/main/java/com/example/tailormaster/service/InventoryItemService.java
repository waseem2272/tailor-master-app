package com.example.tailormaster.service;

import com.example.tailormaster.entity.InventoryItem;
import com.example.tailormaster.entity.InventoryStockBatch;
import com.example.tailormaster.entity.StockMovement;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.enums.ItemType;
import com.example.tailormaster.enums.StockMovementType;
import com.example.tailormaster.enums.StockReferenceType;
import com.example.tailormaster.repository.*;
import com.example.tailormaster.util.AuthenticatedUserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class InventoryItemService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryBrandRepository inventoryBrandRepository;
    private final InventoryCategoryRepository inventoryCategoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryStockBatchRepository inventoryStockBatchRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final InventoryColorRepository inventoryColorRepository;
    private final InventoryDesignRepository inventoryDesignRepository;

    public List<InventoryItem> getAllActive() {
        return inventoryItemRepository.findByUserAndActiveTrue(authenticatedUserService.getCurrentUser());
    }

    public Map<Long, InventoryStockBatch> getCurrentBatchMap(List<InventoryItem> inventoryItems) {
        User user = authenticatedUserService.getCurrentUser();
        Map<Long, InventoryStockBatch> currentBatchMap = new HashMap<>();

        for (InventoryItem item : inventoryItems) {
            List<InventoryStockBatch> batches =
                    inventoryStockBatchRepository
                            .findByUserAndInventoryItemIdAndRemainingQuantityGreaterThanOrderByReceivedDateAscIdAsc(
                                    user, item.getId(), BigDecimal.ZERO);

            if (!batches.isEmpty()) {
                currentBatchMap.put(item.getId(), batches.get(0));
            }
        }

        return currentBatchMap;
    }

    public List<InventoryItem> getAll() {
        return inventoryItemRepository.findAll().stream()
                .filter(item -> item.getUser() != null
                        && item.getUser().getId().equals(authenticatedUserService.getCurrentUser().getId()))
                .toList();
    }

    public InventoryItem getById(Long id) {
        User user = authenticatedUserService.getCurrentUser();
        return inventoryItemRepository.findByUserAndId(user, id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found with id: " + id));
    }

    public List<InventoryItem> getByItemType(ItemType itemType) {
        return inventoryItemRepository.findByUserAndItemTypeAndActiveTrue(
                authenticatedUserService.getCurrentUser(), itemType);
    }

    public List<InventoryItem> search(String name) {
        User user = authenticatedUserService.getCurrentUser();

        if (name == null || name.trim().isEmpty()) {
            return getAllActive();
        }

        return inventoryItemRepository.findByUserAndNameContainingIgnoreCaseAndActiveTrue(
                user, name.trim());
    }

    public InventoryItem save(InventoryItem item) {
        User user = authenticatedUserService.getCurrentUser();
        validateItem(item);
        validateMasterDataOwnership(item, user);

        boolean isNewItem = item.getId() == null;

        if (item.getQuantity() == null) {
            item.setQuantity(BigDecimal.ZERO);
        }
        if (item.getPurchasePrice() == null) {
            item.setPurchasePrice(BigDecimal.ZERO);
        }
        if (item.getSalePrice() == null) {
            item.setSalePrice(BigDecimal.ZERO);
        }
        if (item.getMinimumStock() == null) {
            item.setMinimumStock(BigDecimal.ZERO);
        }
        if (item.getActive() == null) {
            item.setActive(true);
        }

        if (!isNewItem) {
            InventoryItem existingItem = inventoryItemRepository.findByUserAndId(user, item.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Inventory item not found with id: " + item.getId()));

            item.setUser(existingItem.getUser());
            item.setQuantity(existingItem.getQuantity());
        } else {
            item.setUser(user);
        }

        InventoryItem savedItem = inventoryItemRepository.save(item);

        if (isNewItem && savedItem.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            InventoryStockBatch batch = new InventoryStockBatch();
            batch.setInventoryItem(savedItem);
            batch.setUser(authenticatedUserService.getCurrentUser());
            batch.setReceivedQuantity(savedItem.getQuantity());
            batch.setRemainingQuantity(savedItem.getQuantity());
            batch.setPurchasePrice(savedItem.getPurchasePrice());
            batch.setSalePrice(savedItem.getSalePrice());
            batch.setReceivedDate(LocalDateTime.now().withNano(0));
            batch.setActive(true);

            inventoryStockBatchRepository.save(batch);

            log.info("Initial stock batch created. User ID: {}, Item ID: {}, Quantity: {}, Purchase Price: {}, Sale Price: {}",
                    user.getId(), savedItem.getId(), savedItem.getQuantity(),
                    savedItem.getPurchasePrice(), savedItem.getSalePrice());

            StockMovement movement = new StockMovement();
            movement.setInventoryItem(savedItem);
            movement.setUser(authenticatedUserService.getCurrentUser());
            movement.setMovementType(StockMovementType.IN);
            movement.setQuantity(savedItem.getQuantity());
            movement.setUnitPrice(savedItem.getPurchasePrice());
            movement.setReferenceType(StockReferenceType.MANUAL);
            movement.setReason("Initial Stock");
            movement.setNotes("Initial stock added while creating inventory item");

            stockMovementRepository.save(movement);

            log.info("Initial stock movement created. User ID: {}, Item ID: {}, Quantity: {}, Unit Price: {}",
                    user.getId(), savedItem.getId(), savedItem.getQuantity(),
                    savedItem.getPurchasePrice());
        }

        log.info("Inventory item saved successfully. User ID: {}, Item ID: {}, Name: {}, New Item: {}",
                user.getId(), savedItem.getId(), savedItem.getName(), isNewItem);

        return savedItem;
    }

    private void validateMasterDataOwnership(InventoryItem item, User user) {
        if (item.getBrand() != null && item.getBrand().getId() != null) {
            inventoryBrandRepository.findByUserAndId(user, item.getBrand().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid inventory brand"));
        }

        if (item.getCategory() != null && item.getCategory().getId() != null) {
            inventoryCategoryRepository.findByUserAndId(user, item.getCategory().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid inventory category"));
        }

        if (item.getColor() != null && item.getColor().getId() != null) {
            inventoryColorRepository.findByUserAndId(user, item.getColor().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid inventory color"));
        }

        if (item.getDesign() != null && item.getDesign().getId() != null) {
            inventoryDesignRepository.findByUserAndId(user, item.getDesign().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid inventory design"));
        }
    }

    private void validateItem(InventoryItem item) {
        if (item.getName() == null || item.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Item name is required");
        }
        if (item.getItemType() == null) {
            throw new IllegalArgumentException("Item type is required");
        }
        if (item.getUnit() == null) {
            throw new IllegalArgumentException("Unit is required");
        }
        if (item.getQuantity() != null && item.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        if (item.getPurchasePrice() != null && item.getPurchasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Purchase price cannot be negative");
        }
        if (item.getSalePrice() != null && item.getSalePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Sale price cannot be negative");
        }
        if (item.getMinimumStock() != null && item.getMinimumStock().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum stock cannot be negative");
        }
    }

    public void delete(Long id) {
        InventoryItem item = getById(id);
        item.setActive(false);
        inventoryItemRepository.save(item);
        log.info("Inventory item soft deleted. User ID: {}, Item ID: {}", item.getUser().getId(), id);
    }

    public List<InventoryItem> findAllActive() {
        return inventoryItemRepository.findByUserAndActiveTrue(authenticatedUserService.getCurrentUser());
    }

    @Transactional
    public void stockIn(Long inventoryItemId, BigDecimal quantity, BigDecimal purchasePrice, BigDecimal salePrice, String reason, String notes) {
        User user = authenticatedUserService.getCurrentUser();

        if (inventoryItemId == null) {
            throw new IllegalArgumentException("Inventory item is required");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Stock quantity must be greater than zero");
        }
        if (purchasePrice != null && purchasePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Purchase price cannot be negative");
        }
        if (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Sale price cannot be negative");
        }

        InventoryItem item = inventoryItemRepository.findByUserAndId(user, inventoryItemId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory item not found with id: " + inventoryItemId));

        if (Boolean.FALSE.equals(item.getActive())) {
            throw new IllegalArgumentException("Cannot add stock to an inactive inventory item");
        }

        BigDecimal currentQuantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
        BigDecimal newQuantity = currentQuantity.add(quantity);

        item.setQuantity(newQuantity);

        if (purchasePrice != null) {
            item.setPurchasePrice(purchasePrice);
        }

        inventoryItemRepository.save(item);

        InventoryStockBatch batch = new InventoryStockBatch();
        batch.setInventoryItem(item);
        batch.setUser(authenticatedUserService.getCurrentUser());
        batch.setReceivedQuantity(quantity);
        batch.setRemainingQuantity(quantity);
        batch.setPurchasePrice(purchasePrice != null ? purchasePrice : BigDecimal.ZERO);
        batch.setSalePrice(salePrice != null ? salePrice : BigDecimal.ZERO);
        batch.setReceivedDate(LocalDateTime.now().withNano(0));
        batch.setActive(true);

        inventoryStockBatchRepository.save(batch);

        StockMovement movement = new StockMovement();
        movement.setInventoryItem(item);
        movement.setUser(authenticatedUserService.getCurrentUser());
        movement.setMovementType(StockMovementType.IN);
        movement.setQuantity(quantity);
        movement.setUnitPrice(purchasePrice);
        movement.setReason(reason);
        movement.setNotes(notes);
        stockMovementRepository.save(movement);

        log.info("Stock In completed. User ID: {}, Item ID: {}, Batch ID: {}, Quantity: {}, Previous Stock: {}, New Stock: {}",
                user.getId(), inventoryItemId, batch.getId(), quantity, currentQuantity, newQuantity);
    }

    @Transactional
    public void stockOut(Long inventoryItemId, BigDecimal quantity, String reason, String notes) {
        User user = authenticatedUserService.getCurrentUser();

        if (inventoryItemId == null) {
            throw new IllegalArgumentException("Inventory item is required");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Stock quantity must be greater than zero");
        }

        InventoryItem item = inventoryItemRepository.findByUserAndId(user, inventoryItemId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory item not found with id: " + inventoryItemId));

        if (Boolean.FALSE.equals(item.getActive())) {
            throw new IllegalArgumentException("Cannot remove stock from an inactive inventory item");
        }

        BigDecimal currentQuantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;

        if (quantity.compareTo(currentQuantity) > 0) {
            throw new IllegalArgumentException("Stock Out quantity cannot be greater than current stock");
        }

        List<InventoryStockBatch> batches =
                inventoryStockBatchRepository
                        .findByUserAndInventoryItemIdAndRemainingQuantityGreaterThanOrderByReceivedDateAscIdAsc(
                                user, inventoryItemId, BigDecimal.ZERO);

        BigDecimal remainingToRemove = quantity;

        for (InventoryStockBatch batch : batches) {
            if (remainingToRemove.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal batchRemaining = batch.getRemainingQuantity() != null
                    ? batch.getRemainingQuantity()
                    : BigDecimal.ZERO;

            if (batchRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal consumed = remainingToRemove.min(batchRemaining);

            batch.setRemainingQuantity(batchRemaining.subtract(consumed));

            if (batch.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) {
                batch.setActive(false);
            }

            inventoryStockBatchRepository.save(batch);

            remainingToRemove = remainingToRemove.subtract(consumed);

            log.info("FIFO Stock Out batch consumed. User ID: {}, Item ID: {}, Batch ID: {}, Quantity: {}, Remaining Batch Stock: {}",
                    user.getId(), inventoryItemId, batch.getId(), consumed, batch.getRemainingQuantity());
        }

        if (remainingToRemove.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Insufficient batch stock available");
        }

        BigDecimal newQuantity = currentQuantity.subtract(quantity);
        item.setQuantity(newQuantity);
        inventoryItemRepository.save(item);

        StockMovement movement = new StockMovement();
        movement.setInventoryItem(item);
        movement.setUser(authenticatedUserService.getCurrentUser());
        movement.setMovementType(StockMovementType.OUT);
        movement.setQuantity(quantity);
        movement.setReason(reason);
        movement.setNotes(notes);
        stockMovementRepository.save(movement);

        log.info("FIFO Stock Out completed. User ID: {}, Item ID: {}, Quantity: {}, Previous Stock: {}, New Stock: {}",
                user.getId(), inventoryItemId, quantity, currentQuantity, newQuantity);
    }

    public void adjustStock(Long inventoryItemId, BigDecimal newQuantity, String reason, String notes) {
        User user = authenticatedUserService.getCurrentUser();

        if (inventoryItemId == null) {
            throw new IllegalArgumentException("Inventory item is required");
        }
        if (newQuantity == null || newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        InventoryItem item = inventoryItemRepository.findByUserAndId(user, inventoryItemId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory item not found with id: " + inventoryItemId));

        if (Boolean.FALSE.equals(item.getActive())) {
            throw new IllegalArgumentException("Cannot adjust stock of an inactive inventory item");
        }

        BigDecimal currentQuantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;

        if (currentQuantity.compareTo(newQuantity) == 0) {
            throw new IllegalArgumentException("New stock quantity is same as current stock");
        }

        BigDecimal difference = newQuantity.subtract(currentQuantity);

        item.setQuantity(newQuantity);
        inventoryItemRepository.save(item);

        StockMovement movement = new StockMovement();
        movement.setInventoryItem(item);
        movement.setUser(authenticatedUserService.getCurrentUser());
        movement.setMovementType(StockMovementType.ADJUSTMENT);
        movement.setQuantity(difference.abs());
        movement.setReason(reason);
        movement.setNotes(notes);
        stockMovementRepository.save(movement);

        log.info("Stock Adjustment completed. User ID: {}, Item ID: {}, Previous Stock: {}, New Stock: {}, Difference: {}",
                user.getId(), inventoryItemId, currentQuantity, newQuantity, difference);
    }
}