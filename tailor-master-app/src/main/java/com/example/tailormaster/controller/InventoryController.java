package com.example.tailormaster.controller;

import com.example.tailormaster.entity.InventoryItem;
import com.example.tailormaster.entity.InventoryStockBatch;
import com.example.tailormaster.entity.StockMovement;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.enums.*;
import com.example.tailormaster.repository.orderproduct.OrderProductRepository;
import com.example.tailormaster.service.*;
import com.example.tailormaster.service.orderproduct.OrderProductService;
import com.example.tailormaster.util.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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

    @GetMapping
    public String inventoryList(Model model) {
        try {
            log.info("Loading inventory item list");

            List<InventoryItem> inventoryItems =
                    inventoryItemService.getAllActive();

            model.addAttribute(
                    "inventoryItems",
                    inventoryItems
            );

            Map<Long, BigDecimal> reservedStockMap =
                    getReservedStockMap(inventoryItems);

            model.addAttribute(
                    "reservedStockMap",
                    reservedStockMap
            );

            Map<Long, InventoryStockBatch> currentBatchMap =
                    inventoryItemService.getCurrentBatchMap(inventoryItems);

            model.addAttribute("currentBatchMap", currentBatchMap);

            log.info(
                    "Inventory item list loaded successfully. Total items: {}",
                    inventoryItems.size()
            );
            model.addAttribute("activePage", "inventory");
            return "inventory/list";

        } catch (Exception e) {

            log.error(
                    "Error while loading inventory item list",
                    e
            );

            model.addAttribute(
                    "errorMessage",
                    "Unable to load inventory items."
            );

            return "inventory/list";
        }
    }

    private Map<Long, BigDecimal> getReservedStockMap(
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

    // =========================
    // Add Item Form
    // =========================
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
    public String saveItem(@ModelAttribute("inventoryItem") InventoryItem inventoryItem, RedirectAttributes redirectAttributes) {
        try {
            log.info("Saving inventory item. Name: {}, Type: {}", inventoryItem.getName(), inventoryItem.getItemType());
            inventoryItemService.save(inventoryItem);
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

            model.addAttribute("inventoryItem", new InventoryItem());
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
                    getReservedStockMap(inventoryItems);

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
            model.addAttribute("inventoryItems", inventoryItems);
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
            @RequestParam("quantity") BigDecimal quantity,
            @RequestParam("purchasePrice") BigDecimal purchasePrice,
            @RequestParam("salePrice") BigDecimal salePrice,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "notes", required = false) String notes,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Stock In request received. itemId={}, quantity={}, purchasePrice={}, salePrice={}, reason={}",
                    inventoryItemId, quantity, purchasePrice, salePrice, reason);

            inventoryItemService.stockIn(inventoryItemId,
                    quantity,
                    purchasePrice,
                    salePrice,
                    reason,
                    notes);

            log.info("Stock In completed successfully. itemId={}, quantity={}", inventoryItemId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Stock added successfully.");
            return "redirect:/inventory";
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Stock In request. itemId={}, quantity={}, error={}",
                    inventoryItemId, quantity, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/inventory/stock-in";
        } catch (Exception e) {
            log.error("Unexpected error while processing Stock In. itemId={}, quantity={}",
                    inventoryItemId, quantity, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to add stock. Please try again.");
            return "redirect:/inventory/stock-in";
        }
    }

    @GetMapping("/stock-out")
    public String showStockOutPage(Model model) {
        try {
            log.info("Loading Stock Out page");
            List<InventoryItem> inventoryItems = inventoryItemService.findAllActive();
            model.addAttribute("inventoryItems", inventoryItems);
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
            log.info("Loading stock movement history. itemId={}, movementType={}",
                    itemId, movementType);

            List<StockMovement> movements;

            if (itemId != null && movementType != null) {
                movements = stockMovementService.getByItemAndMovementType(
                        itemId,
                        movementType
                );
            } else if (itemId != null) {
                movements = stockMovementService.getByItemId(itemId);
            } else if (movementType != null) {
                movements = stockMovementService.getByMovementType(movementType);
            } else {
                movements = stockMovementService.getAll();
            }

            model.addAttribute("movements", movements);
            model.addAttribute("inventoryItems", inventoryItemService.findAllActive());
            model.addAttribute("movementTypes", StockMovementType.values());
            model.addAttribute("selectedItemId", itemId);
            model.addAttribute("selectedMovementType", movementType);

            log.info("Stock movement history loaded successfully. Total movements: {}",
                    movements.size());
            model.addAttribute("activePage", "inventory/movements");
            return "inventory/movements";
        } catch (Exception e) {
            log.error("Error while loading stock movement history", e);
            model.addAttribute("errorMessage",
                    "Unable to load stock movement history.");
            return "inventory/list";
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

            model.addAttribute("inventoryItems", inventoryItems);
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
            @RequestParam("newQuantity") BigDecimal newQuantity,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "notes", required = false) String notes,
            RedirectAttributes redirectAttributes) {

        try {
            log.info(
                    "Stock Adjustment request received. itemId={}, newQuantity={}, reason={}",
                    inventoryItemId,
                    newQuantity,
                    reason
            );

            inventoryItemService.adjustStock(
                    inventoryItemId,
                    newQuantity,
                    reason,
                    notes
            );

            log.info(
                    "Stock Adjustment completed successfully. itemId={}, newQuantity={}",
                    inventoryItemId,
                    newQuantity
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Stock adjusted successfully."
            );

            return "redirect:/inventory";

        } catch (IllegalArgumentException e) {
            log.warn(
                    "Invalid Stock Adjustment request. itemId={}, newQuantity={}, error={}",
                    inventoryItemId,
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
                    "Unexpected error while adjusting stock. itemId={}, newQuantity={}",
                    inventoryItemId,
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
}