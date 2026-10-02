package com.example.tailormaster.controller;

import com.example.tailormaster.dto.CustomerMeasurementRequest;
import com.example.tailormaster.dto.CustomerMeasurementResponse;
import com.example.tailormaster.dto.CustomerWizardDTO;
import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.CustomerProductMeasurement;
import com.example.tailormaster.entity.ProductMeasurementField;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.service.ProductMeasurementFieldService;
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private final ProductMeasurementFieldService productMeasurementFieldService;

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
            return "customer/create-customer";
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
                productMeasurementFieldService.getMeasurementFieldsByProductId(productId);

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
            return "customer/create-customer";
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
            return "customer/create-customer";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable String id,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
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

            CustomerWizardDTO form = customerService.mapToWizardDTO(customer);

            model.addAttribute("form", form);
            model.addAttribute("customerId", customerId);

            return "customer/edit-customer";

        } catch (Exception ex) {
            logger.error("Error while showing edit customer form: {}", ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Something went wrong while loading edit form."
            );
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
            model.addAttribute("customerId", id);
            return "customer/edit-customer";
        }

        try {
            Customer updatedCustomer = customerService.updateCustomer(id, form);

            logger.info("Customer updated successfully: {}", updatedCustomer);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Customer updated successfully!"
            );

            return "redirect:/customers";

        } catch (Exception ex) {
            logger.error("Error while updating customer: {}", ex.getMessage(), ex);

            model.addAttribute(
                    "errorMessage",
                    "Failed to update customer. Please try again."
            );
            model.addAttribute("customerId", id);

            return "customer/edit-customer";
        }
    }

    @GetMapping("/details/{id}")
    public String showCustomerDetails(
            @PathVariable String id,
            Model model,
            RedirectAttributes redirectAttributes) {

        logger.info("Displaying details for customer ID (encrypted): {}", id);

        try {
            Long customerId = validation.decryptAndValidateId(id);

            if (customerId == null) {
                logger.warn("Invalid customer ID provided: {}", id);
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Invalid customer ID."
                );
                return "redirect:/customers";
            }

            logger.debug("Decrypted customer ID: {}", customerId);

            Customer customer = customerService.getCustomerById(customerId);

            if (customer == null) {
                logger.warn("Customer not found with ID: {}", customerId);
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Customer not found."
                );
                return "redirect:/customers";
            }

            List<CustomerMeasurement> measurements = customer.getMeasurements();

            Map<Product, List<CustomerMeasurement>> productMeasurementsMap =
                    measurements.stream()
                            .collect(Collectors.groupingBy(
                                    CustomerMeasurement::getProduct
                            ));

            List<CustomerProductMeasurement> customerProductMeasurements =
                    measurementService.getCustomerProductMeasurements(customerId);

            model.addAttribute("products", productService.getAllActiveProducts());
            model.addAttribute("customer", customer);
            model.addAttribute("productMeasurementsMap", productMeasurementsMap);
            model.addAttribute("customerProductMeasurements", customerProductMeasurements);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());

            logger.info("Fetched customer details: {}", customer);
            logger.info("Fetched {} customer product measurements for customerId={}", customerProductMeasurements.size(), customerId);

        } catch (Exception e) {
            logger.error("Error loading customer details for ID (encrypted) {}: {}",
                    id, e.getMessage(), e);

            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error loading customer details: " + e.getMessage()
            );

            return "redirect:/customers";
        }

        return "customer/customer-details";
    }

    @PostMapping("/{customerId}/measurements")
    @ResponseBody
    public Map<String, Object> saveCustomerMeasurements(
            @PathVariable Long customerId,
            @RequestParam Long productId,
            @RequestBody CustomerMeasurementRequest request) {

        logger.info("========== SAVE MEASUREMENT ENDPOINT HIT ==========");
        logger.info("customerId={}, productId={}, notes={}, measurements={}",
                customerId,
                productId,
                request.getNotes(),
                request.getMeasurements());

        try {
            measurementService.saveMeasurements(
                    customerId,
                    productId,
                    request.getMeasurements(), request.getNotes());

            logger.info("Measurements saved successfully for customerId={}, productId={}",
                    customerId, productId);

            return Map.of(
                    "success", true,
                    "message", "Measurement saved successfully!"
            );

        } catch (Exception ex) {
            logger.error(
                    "Error saving measurements for customerId={}, productId={}",
                    customerId,
                    productId,
                    ex
            );

            return Map.of(
                    "success", false,
                    "message", "Failed to save measurement."
            );
        }
    }

    @GetMapping("/{customerId}/measurements")
    @ResponseBody
    public List<Map<String, Object>> getCustomerMeasurements(
            @PathVariable Long customerId,
            @RequestParam Long productId) {

        logger.info(
                "Loading measurements for customerId={}, productId={}",
                customerId,
                productId
        );

        List<CustomerMeasurement> measurements =
                measurementService.getMeasurement(customerId, productId);

        return measurements.stream().map(measurement -> {

            Map<String, Object> result = new HashMap<>();

            result.put("fieldId", measurement.getField().getId());
            result.put("value", measurement.getValue());

            return result;

        }).collect(Collectors.toList());
    }

    @GetMapping("/measurements/{measurementId}")
    @ResponseBody
    public Map<String, Object> getCustomerProductMeasurement(@PathVariable Long measurementId) {
        logger.info("========== GET CUSTOMER PRODUCT MEASUREMENT ENDPOINT HIT ==========");
        logger.info("Fetching customer product measurement, measurementId={}", measurementId);
        try {
            CustomerMeasurementResponse measurement = measurementService.getCustomerProductMeasurement(measurementId);
            logger.info("Customer product measurement fetched successfully, measurementId={}, customerId={}, productId={}",
                    measurementId, measurement.getCustomerId(), measurement.getProductId());
            return Map.of("success", true, "measurement", measurement);
        } catch (Exception ex) {
            logger.error("Error fetching customer product measurement, measurementId={}: {}", measurementId, ex.getMessage(), ex);
            return Map.of("success", false, "message", "Failed to load measurement.");
        }
    }

    @PutMapping("/measurements/{measurementId}")
    @ResponseBody
    public Map<String, Object> updateCustomerProductMeasurement(
            @PathVariable Long measurementId,
            @RequestBody CustomerMeasurementRequest request) {

        logger.info("========== UPDATE CUSTOMER PRODUCT MEASUREMENT ENDPOINT HIT ==========");
        logger.info("measurementId={}, notes={}, measurements={}",
                measurementId, request.getNotes(), request.getMeasurements());

        try {
            measurementService.updateCustomerProductMeasurement(
                    measurementId,
                    request.getMeasurements(),
                    request.getNotes()
            );

            logger.info("Customer product measurement updated successfully, measurementId={}",
                    measurementId);

            return Map.of(
                    "success", true,
                    "message", "Measurement updated successfully!"
            );

        } catch (Exception ex) {
            logger.error(
                    "Error updating customer product measurement, measurementId={}: {}",
                    measurementId,
                    ex.getMessage(),
                    ex
            );

            return Map.of(
                    "success", false,
                    "message", "Failed to update measurement."
            );
        }
    }

    @GetMapping("/measurements/{measurementId}/print")
    public String printCustomerMeasurement(@PathVariable Long measurementId,
                                           Model model,
                                           RedirectAttributes redirectAttributes) {

        logger.info("========== CUSTOMER MEASUREMENT PRINT ENDPOINT HIT ==========");
        logger.info("Printing customer measurement, measurementId={}", measurementId);

        try {
            CustomerProductMeasurement measurement =
                    measurementService.getCustomerProductMeasurementEntity(measurementId);

            List<CustomerMeasurement> customerMeasurements =
                    measurementService.getCustomerMeasurementsByParentId(measurementId);

            model.addAttribute("measurement", measurement);
            model.addAttribute("customerMeasurements", customerMeasurements);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            logger.info("Customer measurement print data loaded successfully, measurementId={}",
                    measurementId);

            return "customer/customer-measurement";

        } catch (Exception ex) {
            logger.error(
                    "Error loading customer measurement for print, measurementId={}: {}",
                    measurementId,
                    ex.getMessage(),
                    ex
            );

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Failed to load customer measurement."
            );

            return "redirect:/customers";
        }
    }

    // Delete customer
    @GetMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return "redirect:/customers";
    }
}
