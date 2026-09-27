package com.example.tailormaster.controller;

import com.example.tailormaster.entity.InventoryItem;
import com.example.tailormaster.entity.InventoryStockBatch;
import com.example.tailormaster.entity.StockMovement;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.enums.*;
import com.example.tailormaster.repository.InventoryStockBatchRepository;
import com.example.tailormaster.repository.orderproduct.OrderProductRepository;
import com.example.tailormaster.service.*;
import com.example.tailormaster.service.orderproduct.OrderProductService;
import com.example.tailormaster.util.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Log4j2
public class InventoryController {

    private final InventoryItemService inventoryItemService;
    private final InventoryBrandService inventoryBrandService;
    private final InventoryCategoryService inventoryCategoryService;
    private final StockMovementService stockMovementService;
    private final OrderProductService orderProductService;
    private final AuthenticatedUserService authenticatedUserService;
    private final InventoryColorService inventoryColorService;
    private final InventoryDesignService inventoryDesignService;
    private final OrderInventoryService orderInventoryService;
    private final InventoryStockBatchRepository inventoryStockBatchRepository;

    @GetMapping
    public String inventoryList(Model model) {
        try {
            log.info("Loading inventory item list page");

            model.addAttribute("activePage", "inventory");

            return "inventory/list";

        } catch (Exception e) {
            log.error("Error while loading inventory item list page", e);

            model.addAttribute(
                    "errorMessage",
                    "Unable to load inventory items."
            );

            return "inventory/list";
        }
    }

//    @GetMapping
//    public String inventoryList(Model model) {
//        try {
//            log.info("Loading inventory item list");
//
//            List<InventoryItem> inventoryItems =
//                    inventoryItemService.getAllActive();
//
//            model.addAttribute(
//                    "inventoryItems",
//                    inventoryItems
//            );
//
//            Map<Long, BigDecimal> reservedStockMap =
//                    inventoryItemService.getReservedStockMap(inventoryItems);
//
//            model.addAttribute(
//                    "reservedStockMap",
//                    reservedStockMap
//            );
//
//            Map<Long, BigDecimal> availableStock = inventoryItemService.getAvailableStock(inventoryItems, reservedStockMap);
//            model.addAttribute("availableStock", availableStock);
//
//            Map<Long, InventoryStockBatch> currentBatchMap =
//                    inventoryItemService.getCurrentBatchMap(inventoryItems);
//
//            Map<Long, List<InventoryStockBatch>> stockBatches =
//                    inventoryItemService.getStockBatchesByItems(inventoryItems);
//
//            model.addAttribute("stockBatches", stockBatches);
//
//            model.addAttribute("currentBatchMap", currentBatchMap);
//
//            log.info(
//                    "Inventory item list loaded successfully. Total items: {}",
//                    inventoryItems.size()
//            );
//            model.addAttribute("activePage", "inventory");
//            return "inventory/list";
//
//        } catch (Exception e) {
//
//            log.error(
//                    "Error while loading inventory item list",
//                    e
//            );
//
//            model.addAttribute(
//                    "errorMessage",
//                    "Unable to load inventory items."
//            );
//
//            return "inventory/list";
//        }
//    }

    @GetMapping("/datatable")
    public ResponseEntity<Map<String, Object>> getInventoryDatatable(
            @RequestParam("draw") int draw,
            @RequestParam("start") int start,
            @RequestParam("length") int length,
            @RequestParam(value = "search[value]", required = false) String searchValue,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "itemType", required = false) String itemType,
            @RequestParam(value = "stockStatus", required = false) String stockStatus,
            @RequestParam(value = "order[0][column]", required = false) Integer columnIndex,
            @RequestParam(value = "order[0][dir]", required = false) String sortDirection) {

        try {
            log.info(
                    "Fetching paginated inventory for datatable. draw={}, start={}, length={}, search={}, column={}, direction={}",
                    draw,
                    start,
                    length,
                    searchValue,
                    columnIndex,
                    sortDirection
            );

            Map<String, Object> response =
                    inventoryItemService.getPaginatedInventory(
                            draw,
                            start,
                            length,
                            searchValue,
                            name,
                            itemType,
                            stockStatus,
                            columnIndex,
                            sortDirection
                    );

            log.info(
                    "Inventory datatable fetched successfully. draw={}, response={}",
                    draw,
                    response
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn(
                    "Invalid inventory datatable request: {}",
                    e.getMessage()
            );

            return ResponseEntity.badRequest().body(
                    createErrorResponse(
                            "Invalid request parameters: " + e.getMessage()
                    )
            );

        } catch (Exception e) {
            log.error(
                    "Unexpected error while fetching inventory datatable",
                    e
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    createErrorResponse(
                            "An unexpected error occurred. Please try again."
                    )
            );
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("draw", 0);
        response.put("recordsTotal", 0);
        response.put("recordsFiltered", 0);
        response.put("data", List.of());
        response.put("error", message);
        return response;
    }

    @GetMapping("/add")
    public String addItemForm(Model model) {
        try {
            log.info("Opening add inventory item form");

            User user = authenticatedUserService.getCurrentUser();

            model.addAttribute("inventoryItem", new InventoryItem());
            model.addAttribute("brands", inventoryBrandService.getActiveBrands(user));
            model.addAttribute("categories", inventoryCategoryService.getActiveCategories(user));
            model.addAttribute("colors", inventoryColorService.getActiveColors(user));
            model.addAttribute("designs", inventoryDesignService.getActiveDesigns(user));
            model.addAttribute("itemTypes", ItemType.values());
            model.addAttribute("units", Unit.values());
            model.addAttribute("activePage", "inventory/add");
            return "inventory/add";
        } catch (Exception e) {
            log.error("Error while opening add inventory item form", e);
            model.addAttribute("errorMessage", "Unable to open inventory item form.");
            return "inventory/list";
        }
    }

    // =========================
    // Save Item
    // =========================
    @PostMapping("/save")
    public String saveItem(
            @ModelAttribute("inventoryItem") InventoryItem inventoryItem,
            @RequestParam(value = "batchCode", required = false) String batchCode,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Saving inventory item. Name: {}, Type: {}", inventoryItem.getName(), inventoryItem.getItemType());
            inventoryItemService.save(inventoryItem, batchCode);
            log.info("Inventory item saved successfully. Item ID: {}", inventoryItem.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Stock added successfully.");
            return "redirect:/inventory";
        } catch (Exception e) {
            log.error("Error while saving inventory item. Name: {}", inventoryItem.getName(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to add stock.");
            return "redirect:/inventory/add?error=save";
        }
    }

    // =========================
    // Edit Item
    // =========================
    @GetMapping("/edit/{id}")
    public String editItem(@PathVariable Long id, Model model) {
        try {
            log.info("Opening inventory item for edit. Item ID: {}", id);
            User user = authenticatedUserService.getCurrentUser();

            InventoryItem inventoryItem = inventoryItemService.getById(id);
            model.addAttribute("inventoryItem", inventoryItem);
            model.addAttribute("brands", inventoryBrandService.getActiveBrands(user));
            model.addAttribute("categories", inventoryCategoryService.getActiveCategories(user));
            model.addAttribute("colors", inventoryColorService.getActiveColors(user));
            model.addAttribute("designs", inventoryDesignService.getActiveDesigns(user));
            model.addAttribute("itemTypes", ItemType.values());
            model.addAttribute("units", Unit.values());
            return "inventory/add";
        } catch (Exception e) {
            log.error("Error while loading inventory item for edit. Item ID: {}", id, e);
            return "redirect:/inventory?error=edit";
        }
    }

    // =========================
    // Delete Item
    // =========================
    @GetMapping("/delete/{id}")
    public String deleteItem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            log.info("Deleting inventory item. Item ID: {}", id);
            inventoryItemService.delete(id);
            log.info("Inventory item deleted successfully. Item ID: {}", id);
            redirectAttributes.addFlashAttribute("successMessage", "Item deleted successfully.");
            return "redirect:/inventory";
        } catch (Exception e) {
            log.error("Error while deleting inventory item. Item ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to delete an item.");
            return "redirect:/inventory?error=delete";
        }
    }

    @GetMapping("/search")
    public String search(
            @RequestParam(required = false) String name,
            Model model) {

        try {

            log.info(
                    "Searching inventory items. Search: {}",
                    name
            );

            List<InventoryItem> inventoryItems =
                    inventoryItemService.search(name);

            model.addAttribute(
                    "inventoryItems",
                    inventoryItems
            );

            Map<Long, BigDecimal> reservedStockMap =
                    inventoryItemService.getReservedStockMap(inventoryItems);

            model.addAttribute(
                    "reservedStockMap",
                    reservedStockMap
            );

            return "inventory/list";

        } catch (Exception e) {

            log.error(
                    "Error while searching inventory items. Search: {}",
                    name,
                    e
            );

            model.addAttribute(
                    "errorMessage",
                    "Unable to search inventory items."
            );

            return "inventory/list";
        }
    }

    // =========================
    // Stock In Page
    // =========================
    @GetMapping("/stock-in")
    public String showStockInPage(Model model) {
        try {
            log.info("Loading Stock In page");
            List<InventoryItem> inventoryItems = inventoryItemService.findAllActive();
            Map<Long, List<InventoryStockBatch>> stockBatches = inventoryItemService.getStockBatchesByItems(inventoryItems);
            model.addAttribute("stockBatches", stockBatches);
            Map<Long, BigDecimal> reservedStockMap = inventoryItemService.getReservedStockMap(inventoryItems);
            Map<Long, BigDecimal> availableStock = inventoryItemService.getAvailableStock(inventoryItems, reservedStockMap);
            model.addAttribute("availableStock", availableStock);
            model.addAttribute("inventoryItems", inventoryItems);
            model.addAttribute("reservedStockMap", reservedStockMap);
            model.addAttribute("activePage", "inventory/stock-in");
            return "inventory/stock-in";
        } catch (Exception e) {
            log.error("Error while loading Stock In page", e);
            model.addAttribute("errorMessage", "Unable to load inventory items.");
            return "inventory/stock-in";
        }
    }

    // =========================
    // Stock In
    // =========================
    @PostMapping("/stock-in")
    public String stockIn(
            @RequestParam("inventoryItemId") Long inventoryItemId,
            @RequestParam("batchCode") String batchCode,
            @RequestParam("quantity") BigDecimal quantity,
            @RequestParam("purchasePrice") BigDecimal purchasePrice,
            @RequestParam("salePrice") BigDecimal salePrice,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "notes", required = false) String notes,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Stock In request received. itemId={}, batchCode={}, quantity={}, purchasePrice={}, salePrice={}, reason={}",
                    inventoryItemId, batchCode, quantity, purchasePrice, salePrice, reason);

            inventoryItemService.stockIn(inventoryItemId,
                    batchCode,
                    quantity,
                    purchasePrice,
                    salePrice,
                    reason,
                    notes);

            log.info("Stock In completed successfully. itemId={}, batchCode={}, quantity={}",
                    inventoryItemId, batchCode, quantity);

            redirectAttributes.addFlashAttribute("successMessage", "Stock added successfully.");
            return "redirect:/inventory";
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Stock In request. itemId={}, batchCode={}, quantity={}, error={}",
                    inventoryItemId, batchCode, quantity, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/inventory/stock-in";
        } catch (Exception e) {
            log.error("Unexpected error while processing Stock In. itemId={}, batchCode={}, quantity={}",
                    inventoryItemId, batchCode, quantity, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to add stock. Please try again.");
            return "redirect:/inventory/stock-in";
        }
    }

    @GetMapping("/stock-out")
    public String showStockOutPage(Model model) {
        try {
            log.info("Loading Stock Out page");
            List<InventoryItem> inventoryItems = inventoryItemService.findAllActive();
            Map<Long, BigDecimal> reservedStockMap = inventoryItemService.getReservedStockMap(inventoryItems);
            Map<Long, BigDecimal> availableStock = inventoryItemService.getAvailableStock(inventoryItems, reservedStockMap);
            model.addAttribute("availableStock", availableStock);
            model.addAttribute("inventoryItems", inventoryItems);
            model.addAttribute("reservedStockMap", reservedStockMap);
            model.addAttribute("activePage", "inventory/stock-out");
            return "inventory/stock-out";
        } catch (Exception e) {
            log.error("Error while loading Stock Out page", e);
            model.addAttribute("errorMessage", "Unable to load inventory items.");
            return "inventory/stock-out";
        }
    }

    @PostMapping("/stock-out")
    public String stockOut(
            @RequestParam("inventoryItemId") Long inventoryItemId,
            @RequestParam("quantity") BigDecimal quantity,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "notes", required = false) String notes,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Stock Out request received. itemId={}, quantity={}, reason={}",
                    inventoryItemId, quantity, reason);

            inventoryItemService.stockOut(inventoryItemId, quantity, reason, notes);

            log.info("Stock Out completed successfully. itemId={}, quantity={}",
                    inventoryItemId, quantity);

            redirectAttributes.addFlashAttribute("successMessage", "Stock removed successfully.");
            return "redirect:/inventory";
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Stock Out request. itemId={}, quantity={}, error={}",
                    inventoryItemId, quantity, e.getMessage());

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/inventory/stock-out";
        } catch (Exception e) {
            log.error("Unexpected error while processing Stock Out. itemId={}, quantity={}",
                    inventoryItemId, quantity, e);

            redirectAttributes.addFlashAttribute("errorMessage", "Unable to remove stock. Please try again.");
            return "redirect:/inventory/stock-out";
        }
    }

    @GetMapping("/movements")
    public String stockMovements(
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) StockMovementType movementType,
            Model model) {

        try {
            log.info(
                    "Loading stock movement history page. itemId={}, movementType={}",
                    itemId,
                    movementType
            );

            model.addAttribute(
                    "inventoryItems",
                    inventoryItemService.findAllActive()
            );
            model.addAttribute(
                    "movementTypes",
                    StockMovementType.values()
            );
            model.addAttribute("selectedItemId", itemId);
            model.addAttribute("selectedMovementType", movementType);
            model.addAttribute("activePage", "inventory/movements");

            return "inventory/movements";

        } catch (Exception e) {
            log.error(
                    "Error while loading stock movement history page",
                    e
            );

            model.addAttribute(
                    "errorMessage",
                    "Unable to load stock movement history."
            );

            return "inventory/movements";
        }
    }

    @GetMapping("/movements/datatable")
    public ResponseEntity<Map<String, Object>> getStockMovementsDatatable(
            @RequestParam("draw") int draw,
            @RequestParam("start") int start,
            @RequestParam("length") int length,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) StockMovementType movementType) {

        try {
            log.info(
                    "Fetching paginated stock movements. draw={}, start={}, length={}, itemId={}, movementType={}",
                    draw,
                    start,
                    length,
                    itemId,
                    movementType
            );

            if (start < 0 || length <= 0) {
                throw new IllegalArgumentException(
                        "Start index and length must be greater than zero."
                );
            }

            int page = start / length;

            Page<StockMovement> movementPage =
                    stockMovementService.getPaginated(
                            page,
                            length,
                            itemId,
                            movementType
                    );

            Map<String, Object> response = new HashMap<>();

            response.put("draw", draw);
            response.put("recordsTotal", movementPage.getTotalElements());
            response.put("recordsFiltered", movementPage.getTotalElements());
            List<Map<String, Object>> movementData =
                    movementPage.getContent()
                            .stream()
                            .map(movement -> {

                                Map<String, Object> data = new HashMap<>();

                                data.put(
                                        "createdAt",
                                        movement.getCreatedAt()
                                );

                                data.put(
                                        "inventoryItem",
                                        movement.getInventoryItem() != null
                                                ? Map.of(
                                                "name",
                                                movement.getInventoryItem().getName(),
                                                "unit",
                                                movement.getInventoryItem().getUnit() != null
                                                        ? movement.getInventoryItem().getUnit().name()
                                                        : null
                                        )
                                                : null
                                );

                                data.put(
                                        "movementType",
                                        movement.getMovementType() != null
                                                ? movement.getMovementType().name()
                                                : null
                                );

                                data.put("quantity", movement.getQuantity());
                                data.put("unitPrice", movement.getUnitPrice());
                                data.put("reason", movement.getReason());
                                data.put("notes", movement.getNotes());

                                return data;
                            })
                            .toList();

            response.put("data", movementData);

            log.info(
                    "Stock movements fetched successfully. draw={}, total={}, returned={}",
                    draw,
                    movementPage.getTotalElements(),
                    movementPage.getNumberOfElements()
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn(
                    "Invalid stock movement datatable request: {}",
                    e.getMessage()
            );

            return ResponseEntity.badRequest().body(
                    createErrorResponse(
                            "Invalid request parameters: " + e.getMessage()
                    )
            );

        } catch (Exception e) {
            log.error(
                    "Unexpected error while fetching stock movements",
                    e
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            createErrorResponse(
                                    "An unexpected error occurred. Please try again."
                            )
                    );
        }
    }

    // =========================
// Stock Adjustment Page
// =========================
    @GetMapping("/stock-adjustment")
    public String showStockAdjustmentPage(Model model) {
        try {
            log.info("Loading Stock Adjustment page");

            List<InventoryItem> inventoryItems =
                    inventoryItemService.findAllActive();

            Map<Long, BigDecimal> reservedStockMap = inventoryItemService.getReservedStockMap(inventoryItems);
            Map<Long, List<InventoryStockBatch>> stockBatches = inventoryItemService.getStockBatchesByItems(inventoryItems);
            Map<Long, List<Map<String, Object>>> batchOptions =
                    inventoryItemService.getBatchOptionsByItems(inventoryItems);
            model.addAttribute("batchOptions", batchOptions);
            model.addAttribute("stockBatches", stockBatches);
            Map<Long, BigDecimal> availableStock = inventoryItemService.getAvailableStock(inventoryItems, reservedStockMap);
            model.addAttribute("availableStock", availableStock);
            model.addAttribute("inventoryItems", inventoryItems);
            model.addAttribute("reservedStockMap", reservedStockMap);
            model.addAttribute("activePage", "inventory/stock-adjustment");
            return "inventory/stock-adjustment";
        } catch (Exception e) {
            log.error("Error while loading Stock Adjustment page", e);
            model.addAttribute("errorMessage",
                    "Unable to load inventory items.");
            return "inventory/stock-adjustment";
        }
    }

    // =========================
// Stock Adjustment
// =========================
    @PostMapping("/stock-adjustment")
    public String adjustStock(
            @RequestParam("inventoryItemId") Long inventoryItemId,
            @RequestParam("batchCode") String batchCode,
            @RequestParam("newQuantity") BigDecimal newQuantity,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "notes", required = false) String notes,
            RedirectAttributes redirectAttributes) {

        try {
            log.info(
                    "Stock Adjustment request received. itemId={}, batchCode={}, newQuantity={}, reason={}",
                    inventoryItemId,
                    batchCode,
                    newQuantity,
                    reason
            );

            inventoryItemService.adjustStock(
                    inventoryItemId,
                    batchCode,
                    newQuantity,
                    reason,
                    notes
            );

            log.info(
                    "Stock Adjustment completed successfully. itemId={}, batchCode={}, newQuantity={}",
                    inventoryItemId,
                    batchCode,
                    newQuantity
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Stock adjusted successfully."
            );

            return "redirect:/inventory";

        } catch (IllegalArgumentException e) {
            log.warn(
                    "Invalid Stock Adjustment request. itemId={}, batchCode={}, newQuantity={}, error={}",
                    inventoryItemId,
                    batchCode,
                    newQuantity,
                    e.getMessage()
            );

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/inventory/stock-adjustment";

        } catch (Exception e) {
            log.error(
                    "Unexpected error while adjusting stock. itemId={}, batchCode={}, newQuantity={}",
                    inventoryItemId,
                    batchCode,
                    newQuantity,
                    e
            );

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to adjust stock. Please try again."
            );

            return "redirect:/inventory/stock-adjustment";
        }
    }

    @GetMapping("/batches/{id}")
    public String manageBatches(
            @PathVariable("id") Long inventoryItemId,
            Model model) {

        try {
            log.info("Loading batches for inventory item. itemId={}", inventoryItemId);

            User user = authenticatedUserService.getCurrentUser();

            InventoryItem item = inventoryItemService.getById(inventoryItemId);

            List<InventoryStockBatch> batches =
                    inventoryStockBatchRepository
                            .findByUserAndInventoryItemOrderByReceivedDateAsc(
                                    user,
                                    item
                            );

            model.addAttribute("inventoryItem", item);
            model.addAttribute("batches", batches);
            model.addAttribute("activePage", "inventory");

            return "inventory/batches";

        } catch (Exception e) {
            log.error(
                    "Error while loading inventory batches. itemId={}",
                    inventoryItemId,
                    e
            );

            model.addAttribute(
                    "errorMessage",
                    "Unable to load inventory batches."
            );

            return "inventory/batches";
        }
    }

    @GetMapping("/batches/{batchId}/edit")
    public String editBatchPrice(
            @PathVariable("batchId") Long batchId,
            Model model) {

        try {
            log.info("Loading batch for price edit. batchId={}", batchId);

            User user = authenticatedUserService.getCurrentUser();

            InventoryStockBatch batch =
                    inventoryStockBatchRepository.findById(batchId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Batch / Roll ID not found: " + batchId));

            if (!batch.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("Batch not found.");
            }

            if (batch.getRemainingQuantity() == null
                    || batch.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Consumed batch price cannot be edited.");
            }

            model.addAttribute("batch", batch);
            model.addAttribute("inventoryItem", batch.getInventoryItem());
            model.addAttribute("activePage", "inventory");

            return "inventory/batch-price-edit";

        } catch (Exception e) {
            log.error(
                    "Error while loading batch for price edit. batchId={}",
                    batchId,
                    e
            );

            model.addAttribute(
                    "errorMessage",
                    e.getMessage() != null
                            ? e.getMessage()
                            : "Unable to load batch."
            );

            return "inventory/batches";
        }
    }

    @PostMapping("/batches/{batchId}/edit")
    public String updateBatchPrice(
            @PathVariable("batchId") Long batchId,
            @RequestParam("purchasePrice") BigDecimal purchasePrice,
            @RequestParam("salePrice") BigDecimal salePrice,
            RedirectAttributes redirectAttributes) {

        try {
            log.info(
                    "Batch price update request received. batchId={}, purchasePrice={}, salePrice={}",
                    batchId,
                    purchasePrice,
                    salePrice
            );

            inventoryItemService.updateBatchPrice(
                    batchId,
                    purchasePrice,
                    salePrice
            );

            log.info(
                    "Batch price updated successfully. batchId={}, purchasePrice={}, salePrice={}",
                    batchId,
                    purchasePrice,
                    salePrice
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Batch price updated successfully."
            );

            return "redirect:/inventory/batches/" +
                    inventoryStockBatchRepository.findById(batchId)
                            .map(batch -> batch.getInventoryItem().getId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Batch not found: " + batchId
                            ));

        } catch (IllegalArgumentException e) {
            log.warn(
                    "Invalid batch price update. batchId={}, error={}",
                    batchId,
                    e.getMessage()
            );

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/inventory/batches/" + getInventoryItemId(batchId);

        } catch (Exception e) {
            log.error(
                    "Unexpected error while updating batch price. batchId={}",
                    batchId,
                    e
            );

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to update batch price. Please try again."
            );

            return "redirect:/inventory/batches/" + getInventoryItemId(batchId);
        }
    }

    private Long getInventoryItemId(Long batchId) {
        User user = authenticatedUserService.getCurrentUser();

        return inventoryStockBatchRepository.findById(batchId)
                .filter(batch -> batch.getUser().getId().equals(user.getId()))
                .map(batch -> batch.getInventoryItem().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Batch not found: " + batchId
                ));
    }
}