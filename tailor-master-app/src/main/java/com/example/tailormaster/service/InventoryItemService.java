package com.example.tailormaster.service;

import com.example.tailormaster.entity.InventoryItem;
import com.example.tailormaster.entity.StockMovement;
import com.example.tailormaster.enums.ItemType;
import com.example.tailormaster.enums.StockMovementType;
import com.example.tailormaster.repository.InventoryBrandRepository;
import com.example.tailormaster.repository.InventoryCategoryRepository;
import com.example.tailormaster.repository.InventoryItemRepository;
import com.example.tailormaster.repository.StockMovementRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class InventoryItemService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryBrandRepository inventoryBrandRepository;
    private final InventoryCategoryRepository inventoryCategoryRepository;
    private final StockMovementRepository stockMovementRepository;

    public List<InventoryItem> getAllActive() {
        return inventoryItemRepository.findByActiveTrue();
    }

    public List<InventoryItem> getAll() {
        return inventoryItemRepository.findAll();
    }

    public InventoryItem getById(Long id) {
        return inventoryItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found with id: " + id));
    }

    public List<InventoryItem> getByItemType(ItemType itemType) {
        return inventoryItemRepository.findByItemTypeAndActiveTrue(itemType);
    }

    public List<InventoryItem> search(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getAllActive();
        }
        return inventoryItemRepository.findByNameContainingIgnoreCaseAndActiveTrue(name.trim());
    }

    public InventoryItem save(InventoryItem item) {
        validateItem(item);

        if (item.getQuantity() == null) item.setQuantity(BigDecimal.ZERO);
        if (item.getPurchasePrice() == null) item.setPurchasePrice(BigDecimal.ZERO);
        if (item.getSalePrice() == null) item.setSalePrice(BigDecimal.ZERO);
        if (item.getMinimumStock() == null) item.setMinimumStock(BigDecimal.ZERO);
        if (item.getActive() == null) item.setActive(true);

        InventoryItem savedItem = inventoryItemRepository.save(item);
        log.info("Inventory item saved successfully. Item ID: {}, Name: {}", savedItem.getId(), savedItem.getName());
        return savedItem;
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
        log.info("Inventory item soft deleted. Item ID: {}", id);
    }

    public List<InventoryItem> findAllActive() {
        return inventoryItemRepository.findByActiveTrueOrderByNameAsc();
    }

    public void stockIn(Long inventoryItemId, BigDecimal quantity, BigDecimal unitPrice, String reason, String notes) {
        if (inventoryItemId == null) {
            throw new IllegalArgumentException("Inventory item is required");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Stock quantity must be greater than zero");
        }
        if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }

        InventoryItem item = inventoryItemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found with id: " + inventoryItemId));

        if (Boolean.FALSE.equals(item.getActive())) {
            throw new IllegalArgumentException("Cannot add stock to an inactive inventory item");
        }

        BigDecimal currentQuantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
        BigDecimal newQuantity = currentQuantity.add(quantity);

        item.setQuantity(newQuantity);
        if (unitPrice != null) {
            item.setPurchasePrice(unitPrice);
        }

        inventoryItemRepository.save(item);

        StockMovement movement = new StockMovement();
        movement.setInventoryItem(item);
        movement.setMovementType(StockMovementType.IN);
        movement.setQuantity(quantity);
        movement.setUnitPrice(unitPrice);
        movement.setReason(reason);
        movement.setNotes(notes);
        stockMovementRepository.save(movement);

        log.info("Stock In completed. Item ID: {}, Quantity: {}, Previous Stock: {}, New Stock: {}",
                inventoryItemId, quantity, currentQuantity, newQuantity);
    }

    public void stockOut(Long inventoryItemId, BigDecimal quantity, String reason, String notes) {
        if (inventoryItemId == null) {
            throw new IllegalArgumentException("Inventory item is required");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Stock quantity must be greater than zero");
        }

        InventoryItem item = inventoryItemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found with id: " + inventoryItemId));

        if (Boolean.FALSE.equals(item.getActive())) {
            throw new IllegalArgumentException("Cannot remove stock from an inactive inventory item");
        }

        BigDecimal currentQuantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;

        if (quantity.compareTo(currentQuantity) > 0) {
            throw new IllegalArgumentException("Stock Out quantity cannot be greater than current stock");
        }

        BigDecimal newQuantity = currentQuantity.subtract(quantity);
        item.setQuantity(newQuantity);
        inventoryItemRepository.save(item);

        StockMovement movement = new StockMovement();
        movement.setInventoryItem(item);
        movement.setMovementType(StockMovementType.OUT);
        movement.setQuantity(quantity);
        movement.setReason(reason);
        movement.setNotes(notes);
        stockMovementRepository.save(movement);

        log.info("Stock Out completed. Item ID: {}, Quantity: {}, Previous Stock: {}, New Stock: {}",
                inventoryItemId, quantity, currentQuantity, newQuantity);
    }

    public void adjustStock(Long inventoryItemId, BigDecimal newQuantity, String reason, String notes) {
        if (inventoryItemId == null) {
            throw new IllegalArgumentException("Inventory item is required");
        }

        if (newQuantity == null || newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        InventoryItem item = inventoryItemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory item not found with id: " + inventoryItemId));

        if (Boolean.FALSE.equals(item.getActive())) {
            throw new IllegalArgumentException(
                    "Cannot adjust stock of an inactive inventory item");
        }

        BigDecimal currentQuantity = item.getQuantity() != null
                ? item.getQuantity()
                : BigDecimal.ZERO;

        if (currentQuantity.compareTo(newQuantity) == 0) {
            throw new IllegalArgumentException(
                    "New stock quantity is same as current stock");
        }

        BigDecimal difference = newQuantity.subtract(currentQuantity);

        item.setQuantity(newQuantity);
        inventoryItemRepository.save(item);

        StockMovement movement = new StockMovement();
        movement.setInventoryItem(item);
        movement.setMovementType(StockMovementType.ADJUSTMENT);
        movement.setQuantity(difference.abs());
        movement.setReason(reason);
        movement.setNotes(notes);

        stockMovementRepository.save(movement);

        log.info(
                "Stock Adjustment completed. Item ID: {}, Previous Stock: {}, New Stock: {}, Difference: {}",
                inventoryItemId,
                currentQuantity,
                newQuantity,
                difference
        );
    }
}