package com.example.tailormaster.controller;

import com.example.tailormaster.dto.CustomerWizardDTO;
import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.ProductMeasurementField;
import com.example.tailormaster.entity.ledger.CustomerPaymentLedger;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.enums.OrderProductType;
import com.example.tailormaster.enums.OrderStatus;
import com.example.tailormaster.enums.PaymentType;
import com.example.tailormaster.repository.ProductMeasurementFieldRepository;
import com.example.tailormaster.service.customer.CustomerMeasurementService;
import com.example.tailormaster.service.customer.CustomerService;
import com.example.tailormaster.service.customerledger.CustomerPaymentLedgerService;
import com.example.tailormaster.service.product.ProductService;
import com.example.tailormaster.util.ThymeleafUtil;
import com.example.tailormaster.validation.Validation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@AllArgsConstructor
@Controller
@RequestMapping("/customers")
@SessionAttributes("registrationDTO")  // Store DTO in session
public class CustomerController {

    private static final Logger logger = LogManager.getLogger(CustomerController.class);

    private final CustomerService customerService;
    private final ProductService productService;
    private final CustomerMeasurementService measurementService;
    private final Validation validation;
    private final CustomerPaymentLedgerService customerPaymentLedgerService;
    private final ProductMeasurementFieldRepository fieldRepository;

    // List all customers
    @GetMapping
    public String listCustomers(Model model) {
        try {
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            return "customer/list";
        } catch (Exception e) {
            logger.error("Error loading customer list page", e);
            return "customer/list";
        }
    }

    @GetMapping("/datatable")
    @ResponseBody
    public Map<String, Object> getCustomersData(@RequestParam("draw") int draw,
                                                @RequestParam("start") int start,
                                                @RequestParam("length") int length,
                                                @RequestParam(value = "search[value]", required = false) String searchValue,
                                                @RequestParam(value = "order[0][column]", required = false) Integer columnIndex,
                                                @RequestParam(value = "order[0][dir]", required = false) String sortDirection,
                                                @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            logger.info("Fetching customers data with search value: {}, sort direction: {}, start date: {}, end date: {}",
                    searchValue, sortDirection, startDate, endDate);
            return customerService.getCustomersData(draw, start, length, searchValue, columnIndex, sortDirection, startDate, endDate);
        } catch (Exception e) {
            logger.error("Error retrieving customers from the database", e);
            throw new RuntimeException("Error retrieving customers from the database.", e);
        }
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        try {
            CustomerWizardDTO form = new CustomerWizardDTO();
            model.addAttribute("form", form);
            model.addAttribute("products", productService.getAllActiveProducts());
            model.addAttribute("isEdit", false);
            return "customer/create-customer-wizard";
        } catch (Exception ex) {
            logger.error("Error while showing create customer form: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Something went wrong while loading the form.");
            return "redirect:/customers";
        }
    }

    // Used by front-end (AJAX) to fetch fields for a product
    @GetMapping("/{productId}/fields")
    @ResponseBody
    public List<Map<String, Object>> getFields(@PathVariable Long productId) {
        productService.getProductById(productId);

        List<ProductMeasurementField> fields =
                fieldRepository.findByProductIdOrderByIdAsc(productId);

        return fields.stream().map(field -> {
            Map<String, Object> result = new HashMap<>();
            result.put("id", field.getId());
            result.put("fieldName", field.getFieldName());
            result.put("fieldType", field.getFieldType());
            result.put("options", field.getOptions());
            return result;
        }).collect(Collectors.toList());
    }

    @PostMapping("/create")
    public String createCustomer(
            @Valid @ModelAttribute("form") CustomerWizardDTO form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("products", productService.getAllActiveProducts());
            model.addAttribute("isEdit", false);
            return "customer/create-customer-wizard";
        }

        try {
            Customer savedCustomer = customerService.createCustomerWithMeasurements(form);
            logger.info("Customer created successfully: {}", savedCustomer);
            redirectAttributes.addFlashAttribute("successMessage", "Customer created successfully!");
            return "redirect:/customers";
        } catch (Exception ex) {
            logger.error("Error while creating customer: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Failed to create customer. Please try again.");
            model.addAttribute("products", productService.getAllActiveProducts());
            model.addAttribute("isEdit", false);
            return "customer/create-customer-wizard";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        try {

            // Decrypt and validate customer ID
            Long customerId = validation.validateAndFetchCustomer(id, redirectAttributes);
            if (customerId == null) {
                logger.warn("Invalid customer ID provided for edit: {}", id);
                return "redirect:/customers";
            }

            Customer customer = customerService.getCustomerById(customerId);
            if (customer == null) {
                logger.warn("Customer not found with ID {} for editing.", customerId);
                redirectAttributes.addFlashAttribute("errorMessage", "Customer not found.");
                return "redirect:/customers";
            }
            // Map Customer → DTO
            CustomerWizardDTO form = customerService.mapToWizardDTO(customer);

            model.addAttribute("form", form);
            model.addAttribute("products", productService.getAllActiveProducts());
            model.addAttribute("isEdit", true); // 🔑 flag for Thymeleaf
            model.addAttribute("customerId", customerId);

            return "customer/create-customer-wizard"; // reuse same template
        } catch (Exception ex) {
            logger.error("Error while showing edit customer form: {}", ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Something went wrong while loading edit form.");
            return "redirect:/customers";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateCustomer(@PathVariable Long id,
                                 @Valid @ModelAttribute("form") CustomerWizardDTO form,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("products", productService.getAllActiveProducts());
            model.addAttribute("isEdit", true);
            model.addAttribute("customerId", id);
            return "customer/create-customer-wizard";
        }

        try {
            Customer updatedCustomer = customerService.updateCustomerWithMeasurements(id, form);
            logger.info("Customer updated successfully: {}", updatedCustomer);
            redirectAttributes.addFlashAttribute("successMessage", "Customer updated successfully!");
            return "redirect:/customers";
        } catch (Exception ex) {
            logger.error("Error while updating customer: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Failed to update customer. Please try again.");
            model.addAttribute("products", productService.getAllActiveProducts());
            model.addAttribute("isEdit", true);
            model.addAttribute("customerId", id);
            return "customer/create-customer-wizard";
        }
    }

    @GetMapping("/details/{id}")
    public String showCustomerDetails(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Displaying details for customer ID (encrypted): {}", id);
        try {
            Long customerId = validation.decryptAndValidateId(id);
            if (customerId == null) {
                logger.warn("Invalid customer ID provided: {}", id);
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid customer ID.");
                return "redirect:/customers";
            }

            logger.debug("Decrypted customer ID: {}", customerId);

            Customer customer = customerService.getCustomerById(customerId);
            if (customer == null) {
                logger.warn("Customer not found with ID: {}", customerId);
                redirectAttributes.addFlashAttribute("errorMessage", "Customer not found.");
                return "redirect:/customers";
            }

            List<CustomerMeasurement> measurements = customer.getMeasurements();

            Map<Product, List<CustomerMeasurement>> productMeasurementsMap =
                    measurements.stream()
                            .collect(Collectors.groupingBy(CustomerMeasurement::getProduct));

            Map<String, List<CustomerPaymentLedger>> orderLedgerMap = new LinkedHashMap<>();
            Map<String, BigDecimal> orderBalances = new HashMap<>();
            Map<String, OrderStatus> orderStatuses = new HashMap<>();
            Map<String, Long> orderIds = new HashMap<>();
            Map<String, String> orderTypes = new HashMap<>();

            BigDecimal totalCredit = BigDecimal.ZERO;
            BigDecimal totalDebit = BigDecimal.ZERO;

            List<CustomerPaymentLedger> ledgerEntries =
                    customerPaymentLedgerService.findByCustomerIdOrderByOrderIdAscPaymentDateAsc(customerId);

            for (CustomerPaymentLedger payment : ledgerEntries) {
                Order order = payment.getOrder();
                if (order == null) {
                    continue;
                }

                String orderId = order.getOrderId();

                orderLedgerMap
                        .computeIfAbsent(orderId, k -> new ArrayList<>())
                        .add(payment);

                orderStatuses.put(orderId, order.getStatus());
                orderIds.put(orderId, order.getId());

                if (!orderTypes.containsKey(orderId)) {
                    boolean hasInventory = order.getOrderProducts() != null &&
                            order.getOrderProducts().stream()
                                    .anyMatch(orderProduct ->
                                            orderProduct.getOrderProductType() == OrderProductType.INVENTORY);

                    boolean hasTailoring = order.getOrderProducts() != null &&
                            order.getOrderProducts().stream()
                                    .anyMatch(orderProduct ->
                                            orderProduct.getOrderProductType() != null &&
                                                    orderProduct.getOrderProductType() != OrderProductType.INVENTORY);

                    if (hasInventory && hasTailoring) {
                        orderTypes.put(orderId, "MIXED");
                    } else if (hasInventory) {
                        orderTypes.put(orderId, "INVENTORY");
                    } else {
                        orderTypes.put(orderId, "TAILORING");
                    }
                }

                BigDecimal balance = orderBalances.getOrDefault(orderId, BigDecimal.ZERO);

                if (payment.getPaymentType() == PaymentType.CREDIT) {
                    balance = balance.add(payment.getAmount());
                    totalCredit = totalCredit.add(payment.getAmount());
                } else {
                    balance = balance.subtract(payment.getAmount());
                    totalDebit = totalDebit.add(payment.getAmount());
                }

                orderBalances.put(orderId, balance);
            }

            model.addAttribute("totalCredit", totalCredit);
            model.addAttribute("totalDebit", totalDebit);
            model.addAttribute("orderLedgerMap", orderLedgerMap);
            model.addAttribute("orderBalances", orderBalances);
            model.addAttribute("orderStatuses", orderStatuses);
            model.addAttribute("orderIds", orderIds);
            model.addAttribute("orderTypes", orderTypes);

            model.addAttribute("products", productService.getAllActiveProducts());
            model.addAttribute("customer", customer);
            model.addAttribute("productMeasurementsMap", productMeasurementsMap);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());

            logger.info("Fetched customer details: {}", customer);

        } catch (Exception e) {
            logger.error(
                    "Error loading customer details for ID (encrypted) {}: {}",
                    id,
                    e.getMessage(),
                    e
            );
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Error loading customer details: " + e.getMessage()
            );
            return "redirect:/customers";
        }

        return "customer/customer-details";
    }

    // Delete customer
    @GetMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return "redirect:/customers";
    }
}
