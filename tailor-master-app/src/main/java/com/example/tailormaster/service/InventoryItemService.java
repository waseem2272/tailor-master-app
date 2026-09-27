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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final OrderInventoryService orderInventoryService;

    public Page<InventoryItem> getAllActive(Pageable pageable) {
        User user = authenticatedUserService.getCurrentUser();

        return inventoryItemRepository.findByUserAndActiveTrue(
                user,
                pageable
        );
    }

    public Map<String, Object> getPaginatedInventory(
            int draw,
            int start,
            int length,
            String searchValue,
            String name,
            String itemType,
            String stockStatus,
            Integer columnIndex,
            String sortDirection) {

        try {
            if (start < 0 || length <= 0) {
                throw new IllegalArgumentException(
                        "Start index and length must be greater than zero."
                );
            }

            User user = authenticatedUserService.getCurrentUser();

            String[] columnNames = {
                    "id",
                    "name",
                    "itemType",
                    "brand.name",
                    "category.name",
                    "color.name",
                    "size",
                    "unit",
                    "quantity"
            };

            String sortBy =
                    (columnIndex != null
                            && columnIndex >= 0
                            && columnIndex < columnNames.length)
                            ? columnNames[columnIndex]
                            : "name";

            Sort sort = "desc".equalsIgnoreCase(sortDirection)
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(
                    start / length,
                    length,
                    sort
            );

            Page<InventoryItem> inventoryPage;

            String filterName =
                    name != null && !name.trim().isEmpty()
                            ? name.trim()
                            : null;

            ItemType filterItemType = null;

            if (itemType != null && !itemType.trim().isEmpty()) {
                try {
                    filterItemType = ItemType.valueOf(itemType.trim());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Invalid inventory item type: " + itemType
                    );
                }
            }

            String finalName =
                    filterName != null
                            ? filterName
                            : (searchValue != null && !searchValue.trim().isEmpty()
                            ? searchValue.trim()
                            : null);

            inventoryPage =
                    inventoryItemRepository.findInventoryWithFilters(
                            user,
                            finalName,
                            filterItemType,
                            stockStatus,
                            pageable
                    );

            List<InventoryItem> inventoryItems =
                    inventoryPage.getContent();

            Map<Long, BigDecimal> reservedStockMap =
                    getReservedStockMap(inventoryItems);

            Map<Long, BigDecimal> availableStock =
                    getAvailableStock(
                            inventoryItems,
                            reservedStockMap
                    );

            Map<Long, InventoryStockBatch> currentBatchMap =
                    getCurrentBatchMap(inventoryItems);

            List<Map<String, Object>> inventoryList =
                    inventoryItems.stream()
                            .map(item -> mapInventoryToResponse(
                                    item,
                                    reservedStockMap,
                                    availableStock,
                                    currentBatchMap
                            ))
                            .toList();

            Map<String, Object> response = new HashMap<>();

            response.put("draw", draw);
            response.put("recordsTotal", inventoryPage.getTotalElements());
            response.put("recordsFiltered", inventoryPage.getTotalElements());
            response.put("data", inventoryList);

            log.info(
                    "Fetched paginated inventory. draw={}, start={}, length={}, total={}, returned={}",
                    draw,
                    start,
                    length,
                    inventoryPage.getTotalElements(),
                    inventoryList.size()
            );

            return response;

        } catch (IllegalArgumentException e) {
            log.warn(
                    "Invalid inventory pagination parameters: {}",
                    e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            log.error(
                    "Error processing paginated inventory request",
                    e
            );

            throw new RuntimeException(
                    "Error processing paginated inventory.",
                    e
            );
        }
    }

    private Map<String, Object> mapInventoryToResponse(
            InventoryItem item,
            Map<Long, BigDecimal> reservedStockMap,
            Map<Long, BigDecimal> availableStock,
            Map<Long, InventoryStockBatch> currentBatchMap) {

        Map<String, Object> data = new HashMap<>();

        BigDecimal physical =
                item.getQuantity() != null
                        ? item.getQuantity()
                        : BigDecimal.ZERO;

        BigDecimal reserved =
                reservedStockMap.get(item.getId()) != null
                        ? reservedStockMap.get(item.getId())
                        : BigDecimal.ZERO;

        BigDecimal available =
                availableStock.get(item.getId()) != null
                        ? availableStock.get(item.getId())
                        : BigDecimal.ZERO;

        InventoryStockBatch currentBatch =
                currentBatchMap.get(item.getId());

        data.put("id", item.getId());
        data.put("itemName", item.getName());

        data.put(
                "type",
                item.getItemType() != null
                        ? item.getItemType().name().replace("_", " ")
                        : "—"
        );

        data.put(
                "brand",
                item.getBrand() != null
                        ? item.getBrand().getName()
                        : "—"
        );

        data.put(
                "category",
                item.getCategory() != null
                        ? item.getCategory().getName()
                        : "—"
        );

        data.put(
                "color",
                item.getColor() != null
                        ? item.getColor().getName()
                        : "—"
        );

        data.put(
                "size",
                item.getSize() != null && !item.getSize().isEmpty()
                        ? item.getSize()
                        : "—"
        );

        data.put(
                "unit",
                item.getUnit() != null
                        ? item.getUnit().name().replace("_", " ")
                        : "—"
        );

        data.put("physical", formatQuantity(physical));
        data.put("reserved", formatQuantity(reserved));
        data.put("available", formatQuantity(available));

        data.put(
                "purchasePrice",
                currentBatch != null
                        ? currentBatch.getPurchasePrice()
                        : null
        );

        data.put(
                "salePrice",
                currentBatch != null
                        ? currentBatch.getSalePrice()
                        : null
        );

        String status;

        if (physical.compareTo(BigDecimal.ZERO) == 0) {
            status = "Out of Stock";
        } else if (available.compareTo(BigDecimal.ZERO) == 0) {
            status = "Reserved";
        } else if (item.getMinimumStock() != null
                && available.compareTo(item.getMinimumStock()) <= 0) {
            status = "Low Stock";
        } else {
            status = "Available";
        }

        data.put("status", status);

        data.put(
                "actions",
                "<div class=\"d-flex gap-1 justify-content-center\">"
                        + "<a href=\"/inventory/batches/" + item.getId()
                        + "\" class=\"btn btn-info btn-sm text-white\" title=\"Manage Batches\">"
                        + "<i class=\"bi bi-layers\"></i>"
                        + "</a>"
                        + "<a href=\"/inventory/edit/" + item.getId()
                        + "\" class=\"btn btn-warning btn-sm\" title=\"Edit\">"
                        + "<i class=\"bi bi-pencil\"></i>"
                        + "</a>"
                        + "<a href=\"/inventory/delete/" + item.getId()
                        + "\" class=\"btn btn-danger btn-sm\" title=\"Delete\" "
                        + "onclick=\"return confirm('Are you sure you want to delete this item?');\">"
                        + "<i class=\"bi bi-trash\"></i>"
                        + "</a>"
                        + "</div>"
        );

        return data;
    }

    private String formatQuantity(BigDecimal quantity) {
        return quantity == null
                ? "0"
                : quantity.stripTrailingZeros().toPlainString();
    }

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

    public InventoryItem save(InventoryItem item, String batchCode) {        User user = authenticatedUserService.getCurrentUser();
        validateItem(item);
        validateMasterDataOwnership(item, user);

        boolean isNewItem = item.getId() == null;
        if (isNewItem) {
            if (batchCode == null || batchCode.trim().isEmpty()) {
                throw new IllegalArgumentException("Batch / Roll ID is required");
            }

            batchCode = batchCode.trim();

            if (inventoryStockBatchRepository.findByUserAndBatchCode(user, batchCode).isPresent()) {
                throw new IllegalArgumentException("Batch / Roll ID already exists");
            }
        }

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
            batch.setBatchCode(batchCode);
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
            movement.setNotes("Initial stock added while creating inventory item. Batch Code / Roll: " + batchCode);

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
    public void stockIn(Long inventoryItemId, String batchCode, BigDecimal quantity, BigDecimal purchasePrice, BigDecimal salePrice, String reason, String notes) {        User user = authenticatedUserService.getCurrentUser();

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
        if (batchCode == null || batchCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch / Roll ID is required");
        }
        batchCode = batchCode.trim();

        if (inventoryStockBatchRepository.findByUserAndBatchCode(user, batchCode).isPresent()) {
            throw new IllegalArgumentException("Batch / Roll ID already exists");
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
        batch.setBatchCode(batchCode);
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

        log.info("Stock In completed. User ID: {}, Item ID: {}, Batch ID: {}, Batch Code: {}, Quantity: {}, Previous Stock: {}, New Stock: {}",
                user.getId(), inventoryItemId, batch.getId(), batchCode, quantity, currentQuantity, newQuantity);
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

    public void adjustStock(Long inventoryItemId, String batchCode, BigDecimal newQuantity, String reason, String notes) {
        User user = authenticatedUserService.getCurrentUser();

        if (inventoryItemId == null) {
            throw new IllegalArgumentException("Inventory item is required");
        }
        if (batchCode == null || batchCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch / Roll ID is required");
        }
        if (newQuantity == null || newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        batchCode = batchCode.trim();

        InventoryItem item = inventoryItemRepository.findByUserAndId(user, inventoryItemId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory item not found with id: " + inventoryItemId));

        if (Boolean.FALSE.equals(item.getActive())) {
            throw new IllegalArgumentException("Cannot adjust stock of an inactive inventory item");
        }

        String trimmedBatchCode = batchCode.trim();

        InventoryStockBatch batch = inventoryStockBatchRepository.findByUserAndBatchCode(user, batchCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Batch / Roll ID not found: " + trimmedBatchCode));

        if (!batch.getInventoryItem().getId().equals(inventoryItemId)) {
            throw new IllegalArgumentException(
                    "Selected Batch / Roll ID does not belong to the selected inventory item");
        }

        BigDecimal currentBatchQuantity = batch.getRemainingQuantity() != null
                ? batch.getRemainingQuantity()
                : BigDecimal.ZERO;

        if (currentBatchQuantity.compareTo(newQuantity) == 0) {
            throw new IllegalArgumentException("New stock quantity is same as current batch stock");
        }

        BigDecimal difference = newQuantity.subtract(currentBatchQuantity);

        batch.setRemainingQuantity(newQuantity);
        batch.setActive(newQuantity.compareTo(BigDecimal.ZERO) > 0);
        inventoryStockBatchRepository.save(batch);

        BigDecimal currentItemQuantity = item.getQuantity() != null
                ? item.getQuantity()
                : BigDecimal.ZERO;

        BigDecimal newItemQuantity = currentItemQuantity.add(difference);

        if (newItemQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Adjusted stock cannot make total inventory quantity negative");
        }

        item.setQuantity(newItemQuantity);
        inventoryItemRepository.save(item);

        StockMovement movement = new StockMovement();
        movement.setInventoryItem(item);
        movement.setUser(user);
        movement.setMovementType(StockMovementType.ADJUSTMENT);
        movement.setQuantity(difference.abs());
        movement.setReason(reason);
        movement.setNotes(notes);
        stockMovementRepository.save(movement);

        log.info(
                "Stock Adjustment completed. User ID: {}, Item ID: {}, Batch Code: {}, Previous Batch Stock: {}, New Batch Stock: {}, Difference: {}, Previous Item Stock: {}, New Item Stock: {}",
                user.getId(),
                inventoryItemId,
                trimmedBatchCode,
                currentBatchQuantity,
                newQuantity,
                difference,
                currentItemQuantity,
                newItemQuantity
        );
    }

    public Map<Long, BigDecimal> getAvailableStock(List<InventoryItem> inventoryItems, Map<Long, BigDecimal> reservedStockMap) {
        Map<Long, BigDecimal> availableStockMap = new HashMap<>();

        for (InventoryItem item : inventoryItems) {
            BigDecimal stock = item.getQuantity() != null
                    ? item.getQuantity()
                    : BigDecimal.ZERO;

            BigDecimal reserved = reservedStockMap.get(item.getId());

            if (reserved == null) {
                reserved = BigDecimal.ZERO;
            }

            BigDecimal available = stock.subtract(reserved).max(BigDecimal.ZERO);

            availableStockMap.put(item.getId(), available);
        }
        return availableStockMap;
    }

    public Map<Long, List<InventoryStockBatch>> getStockBatchesByItems(List<InventoryItem> inventoryItems) {
        User user = authenticatedUserService.getCurrentUser();
        Map<Long, List<InventoryStockBatch>> stockBatchesMap = new HashMap<>();

        for (InventoryItem item : inventoryItems) {
            List<InventoryStockBatch> batches =
                    inventoryStockBatchRepository
                            .findByUserAndInventoryItemIdAndRemainingQuantityGreaterThanOrderByReceivedDateAscIdAsc(
                                    user, item.getId(), BigDecimal.ZERO);

            stockBatchesMap.put(item.getId(), batches);
        }

        return stockBatchesMap;
    }

    public Map<Long, List<Map<String, Object>>> getBatchOptionsByItems(List<InventoryItem> inventoryItems) {
        User user = authenticatedUserService.getCurrentUser();
        Map<Long, List<Map<String, Object>>> batchOptionsMap = new HashMap<>();

        for (InventoryItem item : inventoryItems) {
            List<InventoryStockBatch> batches =
                    inventoryStockBatchRepository
                            .findByUserAndInventoryItemIdAndRemainingQuantityGreaterThanOrderByReceivedDateAscIdAsc(
                                    user, item.getId(), BigDecimal.ZERO);

            List<Map<String, Object>> batchOptions = new ArrayList<>();

            for (InventoryStockBatch batch : batches) {
                Map<String, Object> batchData = new HashMap<>();
                batchData.put("batchCode", batch.getBatchCode());
                batchData.put("remainingQuantity", batch.getRemainingQuantity());
                batchOptions.add(batchData);
            }

            batchOptionsMap.put(item.getId(), batchOptions);
        }

        return batchOptionsMap;
    }

    public void updateBatchPrice(
            Long batchId,
            BigDecimal purchasePrice,
            BigDecimal salePrice) {

        User user = authenticatedUserService.getCurrentUser();

        if (batchId == null) {
            throw new IllegalArgumentException("Batch is required");
        }

        if (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Purchase price cannot be negative");
        }

        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Sale price cannot be negative");
        }

        InventoryStockBatch batch =
                inventoryStockBatchRepository.findById(batchId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Batch not found: " + batchId));

        if (!batch.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Batch not found.");
        }

        if (batch.getRemainingQuantity() == null
                || batch.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Consumed batch price cannot be edited.");
        }

        BigDecimal oldPurchasePrice = batch.getPurchasePrice();
        BigDecimal oldSalePrice = batch.getSalePrice();

        batch.setPurchasePrice(purchasePrice);
        batch.setSalePrice(salePrice);

        inventoryStockBatchRepository.save(batch);

        log.info(
                "Batch price updated. User ID: {}, Batch ID: {}, Batch Code: {}, Old Purchase Price: {}, New Purchase Price: {}, Old Sale Price: {}, New Sale Price: {}, Remaining Quantity: {}",
                user.getId(),
                batch.getId(),
                batch.getBatchCode(),
                oldPurchasePrice,
                purchasePrice,
                oldSalePrice,
                salePrice,
                batch.getRemainingQuantity()
        );
    }

    public Map<Long, BigDecimal> getReservedStockMap(
            List<InventoryItem> inventoryItems) {

        Map<Long, BigDecimal> reservedStockMap =
                new HashMap<>();

        for (InventoryItem item : inventoryItems) {

            BigDecimal reservedQuantity = BigDecimal.ZERO;

//            if (item.getItemType() == ItemType.FABRIC) {
            reservedQuantity =
                    orderInventoryService
                            .getReservedQuantity(item.getId());
//            }

            reservedStockMap.put(
                    item.getId(),
                    reservedQuantity
            );
        }

        return reservedStockMap;
    }
}