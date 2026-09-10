package com.example.tailormaster.controller;

import com.example.tailormaster.entity.InventoryItem;
import com.example.tailormaster.entity.StockMovement;
import com.example.tailormaster.enums.ItemType;
import com.example.tailormaster.enums.StockMovementType;
import com.example.tailormaster.enums.Unit;
import com.example.tailormaster.service.InventoryBrandService;
import com.example.tailormaster.service.InventoryCategoryService;
import com.example.tailormaster.service.InventoryItemService;
import com.example.tailormaster.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Log4j2
public class InventoryController {

    private final InventoryItemService inventoryItemService;
    private final InventoryBrandService inventoryBrandService;
    private final InventoryCategoryService inventoryCategoryService;
    private final StockMovementService stockMovementService;

    // =========================
    // Inventory List
    // =========================
    @GetMapping
    public String inventoryList(Model model) {
        try {
            log.info("Loading inventory item list");
            model.addAttribute("inventoryItems", inventoryItemService.getAllActive());
            log.info("Inventory item list loaded successfully");
            return "inventory/list";
        } catch (Exception e) {
            log.error("Error while loading inventory item list", e);
            model.addAttribute("errorMessage", "Unable to load inventory items.");
            return "error";
        }
    }

    // =========================
    // Add Item Form
    // =========================
    @GetMapping("/add")
    public String addItemForm(Model model) {
        try {
            log.info("Opening add inventory item form");
            model.addAttribute("inventoryItem", new InventoryItem());
            model.addAttribute("brands", inventoryBrandService.getAllActive());
            model.addAttribute("categories", inventoryCategoryService.getAllActive());
            model.addAttribute("itemTypes", ItemType.values());
            model.addAttribute("units", Unit.values());
            return "inventory/add";
        } catch (Exception e) {
            log.error("Error while opening add inventory item form", e);
            model.addAttribute("errorMessage", "Unable to open inventory item form.");
            return "error";
        }
    }

    // =========================
    // Save Item
    // =========================
    @PostMapping("/save")
    public String saveItem(@ModelAttribute("inventoryItem") InventoryItem inventoryItem) {
        try {
            log.info("Saving inventory item. Name: {}, Type: {}", inventoryItem.getName(), inventoryItem.getItemType());
            inventoryItemService.save(inventoryItem);
            log.info("Inventory item saved successfully. Item ID: {}", inventoryItem.getId());
            return "redirect:/inventory";
        } catch (Exception e) {
            log.error("Error while saving inventory item. Name: {}", inventoryItem.getName(), e);
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
            model.addAttribute("inventoryItem", inventoryItemService.getById(id));
            model.addAttribute("brands", inventoryBrandService.getAllActive());
            model.addAttribute("categories", inventoryCategoryService.getAllActive());
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
    public String deleteItem(@PathVariable Long id) {
        try {
            log.info("Deleting inventory item. Item ID: {}", id);
            inventoryItemService.delete(id);
            log.info("Inventory item deleted successfully. Item ID: {}", id);
            return "redirect:/inventory";
        } catch (Exception e) {
            log.error("Error while deleting inventory item. Item ID: {}", id, e);
            return "redirect:/inventory?error=delete";
        }
    }

    // =========================
    // Search
    // =========================
    @GetMapping("/search")
    public String search(@RequestParam(required = false) String name, Model model) {
        try {
            log.info("Searching inventory items. Search: {}", name);
            model.addAttribute("inventoryItems", inventoryItemService.search(name));
            return "inventory/list";
        } catch (Exception e) {
            log.error("Error while searching inventory items. Search: {}", name, e);
            model.addAttribute("errorMessage", "Unable to search inventory items.");
            return "error";
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
            @RequestParam(value = "unitPrice", required = false) BigDecimal unitPrice,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "notes", required = false) String notes,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("Stock In request received. itemId={}, quantity={}, unitPrice={}, reason={}",
                    inventoryItemId, quantity, unitPrice, reason);

            inventoryItemService.stockIn(inventoryItemId, quantity, unitPrice, reason, notes);

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

    // =========================
// Stock Out Page
// =========================
    @GetMapping("/stock-out")
    public String showStockOutPage(Model model) {
        try {
            log.info("Loading Stock Out page");
            List<InventoryItem> inventoryItems = inventoryItemService.findAllActive();
            model.addAttribute("inventoryItems", inventoryItems);
            return "inventory/stock-out";
        } catch (Exception e) {
            log.error("Error while loading Stock Out page", e);
            model.addAttribute("errorMessage", "Unable to load inventory items.");
            return "inventory/stock-out";
        }
    }

    // =========================
// Stock Out
// =========================
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
    // =========================
// Stock Movement History
// =========================
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

            return "inventory/movements";
        } catch (Exception e) {
            log.error("Error while loading stock movement history", e);
            model.addAttribute("errorMessage",
                    "Unable to load stock movement history.");
            return "error";
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