package com.example.tailormaster.controller;

import com.example.tailormaster.dto.CustomerRegistrationDTO;
import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.ledger.CustomerPaymentLedger;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.enums.PaymentType;
import com.example.tailormaster.service.customer.CustomerMeasurementService;
import com.example.tailormaster.service.customer.CustomerService;
import com.example.tailormaster.service.customerledger.CustomerPaymentLedgerService;
import com.example.tailormaster.service.product.ProductService;
import com.example.tailormaster.util.ThymeleafUtil;
import com.example.tailormaster.validation.Utility;
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

    // Show create customer form
    @GetMapping("/create")
    public String showCreateCustomerForm(Model model) {
        try {
            model.addAttribute("customer", new Customer());
            CustomerRegistrationDTO registrationDTO = populateCustomerRegistrationDTO();
            model.addAttribute("registrationDTO", registrationDTO);
            return "customer/create";
        } catch (Exception e) {
            logger.error("Error loading customer creation form", e);
            return "redirect:/customers";
        }
    }

    // create customer
    @PostMapping("/create")
    public String createCustomer(
            @Valid @ModelAttribute CustomerRegistrationDTO registrationDTO,
            BindingResult result,
            @RequestParam(value = "productIds", required = false) Long[] productIds,
            RedirectAttributes redirectAttributes,
            Model model) {

        logger.info("Creating customer with registration data: {}", registrationDTO);

        // Validate selected products
        if (productIds == null || productIds.length == 0) {
            result.rejectValue("products", Utility.PRODUCT_ERROR_CODE, Utility.PRODUCT_ERROR_MESSAGE);
        }

        // Validate measurements dynamically based on selected products
        Map<Long, CustomerMeasurement> customerMeasurements = registrationDTO.getCustomerMeasurements();
        // Store only selected ones
        if (productIds != null) {
            for (Long productId : productIds) {
                CustomerMeasurement measurement = customerMeasurements.get(productId);
                if (measurement == null) {
                    result.rejectValue("customerMeasurements", "error.measurements", "Measurements are required for the selected product.");
                } else {
                    Product product = productService.getProductById(productId);
                    validation.validateMeasurement(product, measurement, result);
                }
            }
        }
        Set<Long> selectedProductIds = productIds != null ? Set.of(productIds) : new HashSet<>();
        // If validation fails, return with error messages
        if (result.hasErrors()) {
            CustomerRegistrationDTO tempRegistrationDTO = populateCustomerRegistrationDTO(registrationDTO);

            // Extract selected product IDs
//            Set<Long> selectedProductIds = productIds != null ? Set.of(productIds) : new HashSet<>();
            tempRegistrationDTO.setSelectedProductIds(selectedProductIds);
            model.addAttribute("registrationDTO", tempRegistrationDTO);
            model.addAttribute("org.springframework.validation.BindingResult.registrationDTO", result);
            return "customer/create";
        }

        try {
            // Save customer details
            Customer savedCustomer = customerService.createCustomer(registrationDTO.getCustomer());

            // Save measurements for each selected product
            for (Long productId : selectedProductIds) {
                Product product = productService.getProductById(productId);
                saveCustomerMeasurement(savedCustomer, product, customerMeasurements.get(productId));
            }

            // Success message
            logger.info("Customer created successfully with ID: {}", savedCustomer.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Customer created successfully.");
            redirectAttributes.addFlashAttribute("customerId", savedCustomer.getId());
            return "redirect:/customers";

        } catch (Exception e) {
            logger.error("Error creating customer", e);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while creating the customer.");
            return "redirect:/customer/create";
        }
    }

    // generate barcode
    private static String generateBarcode(Customer savedCustomer) {
        return savedCustomer.getFullName() + savedCustomer.getPhoneNumber() + "-" + UUID.randomUUID();
    }

    // Utility method to save customer measurements
    private void saveCustomerMeasurement(Customer customer, Product product, CustomerMeasurement providedMeasurement) {
        try {
            CustomerMeasurement measurement = new CustomerMeasurement();
            measurement.setCustomer(customer);
            measurement.setProduct(product);
            measurement.setChest(providedMeasurement.getChest());
            measurement.setSleeveLength(providedMeasurement.getSleeveLength());
            measurement.setShoulder(providedMeasurement.getShoulder());
            measurement.setHips(providedMeasurement.getHips());
            measurement.setWaist(providedMeasurement.getWaist());

            // Generate barcode
            String barcode = generateBarcode(customer);
            measurement.setBarcode(barcode);

            measurementService.saveMeasurement(measurement);
        } catch (Exception e) {
            logger.error("Error saving customer measurement", e);
        }
    }

    private CustomerRegistrationDTO populateCustomerRegistrationDTO() {
        try {
            CustomerRegistrationDTO registrationDTO = new CustomerRegistrationDTO();
            registrationDTO.setProducts(productService.getAllActiveProducts());

            // Create a new CustomerMeasurement for each product
            for (Product product : registrationDTO.getProducts()) {
                CustomerMeasurement customerMeasurement = new CustomerMeasurement();
                registrationDTO.addCustomerMeasurement(product.getId(), customerMeasurement);
            }
            return registrationDTO;
        } catch (Exception e) {
            logger.error("Error populating customer registration DTO", e);
            return new CustomerRegistrationDTO();
        }
    }

    private CustomerRegistrationDTO populateCustomerRegistrationDTO(CustomerRegistrationDTO existingDTO) {
        try {
            CustomerRegistrationDTO registrationDTO = new CustomerRegistrationDTO();
            registrationDTO.setCustomer(existingDTO.getCustomer());
            registrationDTO.setProducts(productService.getAllActiveProducts());

            // Retain already entered customer measurements
            for (Product product : registrationDTO.getProducts()) {
                if (existingDTO.getCustomerMeasurements().containsKey(product.getId())) {
                    // Keep the entered values
                    registrationDTO.addCustomerMeasurement(product.getId(), existingDTO.getCustomerMeasurements().get(product.getId()));
                } else {
                    // Otherwise, initialize a new empty measurement
                    registrationDTO.addCustomerMeasurement(product.getId(), new CustomerMeasurement());
                }
            }
            return registrationDTO;
        } catch (Exception e) {
            logger.error("Error populating customer registration DTO", e);
            return new CustomerRegistrationDTO();
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditCustomerForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Displaying edit form for customer ID (encrypted): {}", id);
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
            CustomerRegistrationDTO registrationDTO = populateEditCustomerRegistrationDTO(customer);
            model.addAttribute("registrationDTO", registrationDTO);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            logger.info("Populated CustomerRegistrationDTO for edit form: {}", registrationDTO);
            return "customer/update";

        } catch (Exception e) {
            logger.error("Error loading customer edit form for ID (encrypted) {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error loading customer edit: " + e.getMessage());
            return "redirect:/customers"; // Keep only one return statement
        }
    }

    private CustomerRegistrationDTO populateEditCustomerRegistrationDTO(Customer customer) {
        CustomerRegistrationDTO registrationDTO = new CustomerRegistrationDTO();

        // Set customer info
        registrationDTO.setCustomer(customer);

        // Fetch all active products
        List<Product> allActiveProducts = productService.getAllActiveProducts();

        // Fetch products the customer has selected
        Set<Long> selectedProductIds = customer.getMeasurements()
                .stream()
                .map(measurement -> measurement.getProduct().getId())
                .collect(Collectors.toSet());

        // Ensure all active products are listed, marking selected ones
        List<Product> productsList = new ArrayList<>();
        for (Product product : allActiveProducts) {
            productsList.add(product); // Add all active products (both selected & unselected)
        }
        registrationDTO.setProducts(productsList);

        // Populate customer measurements
        Map<Long, CustomerMeasurement> measurementMap = new HashMap<>();
        for (CustomerMeasurement measurement : customer.getMeasurements()) {
            measurementMap.put(measurement.getProduct().getId(), measurement);
        }
        registrationDTO.setCustomerMeasurements(measurementMap);

        // Pass the selected product IDs for Thymeleaf to check the right boxes
        registrationDTO.setSelectedProductIds(selectedProductIds);

        return registrationDTO;
    }

    // update customer
    @PostMapping("/update")
    public String updateCustomer(
            @Valid @ModelAttribute CustomerRegistrationDTO registrationDTO,
            BindingResult result,
            @RequestParam(value = "productIds", required = false) Long[] productIds,
            @RequestParam("encryptedCustomerId") String encryptedCustomerId,
            RedirectAttributes redirectAttributes,
            Model model) {
        logger.info("Attempting to update customer with encrypted ID: {}", encryptedCustomerId);
        try {
            // Decrypt and validate customer ID
            Long customerId = validation.validateAndFetchCustomer(encryptedCustomerId, redirectAttributes);
            if (customerId == null) {
                logger.warn("Invalid customer ID provided for update: {}", encryptedCustomerId);
                return "redirect:/customers";
            }
            logger.debug("Decrypted customer ID for update: {}", customerId);

            // Validate selected products
            if (productIds == null || productIds.length == 0) {
                logger.warn("No products selected for customer update.");
                result.rejectValue("products", Utility.PRODUCT_ERROR_CODE, Utility.PRODUCT_ERROR_MESSAGE);
            }

            List<Product> selectedProducts = new ArrayList<>();
            logger.debug("Selected product IDs: {}", productIds);

            // Validate measurements dynamically based on selected products
            Map<Long, CustomerMeasurement> customerMeasurements = registrationDTO.getCustomerMeasurements();
            logger.debug("Customer measurements provided: {}", customerMeasurements);
            // Store only selected ones
            if (productIds != null) {
                for (Long productId : productIds) {
                    CustomerMeasurement measurement = customerMeasurements.get(productId);
                    if (measurement == null) {
                        logger.warn("Measurement missing for selected product ID: {}", productId);
                        result.rejectValue("customerMeasurements", "error.measurements", "Measurements are required for the selected product.");
                    } else {
                        Product product = productService.getProductById(productId);
                        validation.validateMeasurement(product, measurement, result);
                        selectedProducts.add(product);
                    }
                }
            }
            logger.debug("Number of selected products after validation: {}", selectedProducts.size());
            // set decrypted customer id
            registrationDTO.getCustomer().setId(customerId);

            // If validation fails, return with error messages
            if (result.hasErrors()) {
                logger.warn("Validation errors occurred during customer update for ID {}: {}", customerId, result.getAllErrors());
                CustomerRegistrationDTO tempRegistrationDTO = populateCustomerRegistrationDTO(registrationDTO);

                // Extract selected product IDs
                Set<Long> selectedProductIds = productIds != null ? Set.of(productIds) : new HashSet<>();
                tempRegistrationDTO.setSelectedProductIds(selectedProductIds);
                model.addAttribute("registrationDTO", tempRegistrationDTO);
                model.addAttribute("thymeleafUtil", new ThymeleafUtil());
                model.addAttribute("org.springframework.validation.BindingResult.registrationDTO", result);
                return "customer/update";
            }

            Customer updatedCustomer = customerService.updateCustomer(registrationDTO, selectedProducts);
            logger.info("Customer updated successfully with ID: {}", updatedCustomer.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Customer updated successfully!");
            redirectAttributes.addFlashAttribute("customerId", updatedCustomer.getId());
            return "redirect:/customers";
        } catch (Exception e) {
            logger.error("Error updating customer with encrypted ID {}: {}", encryptedCustomerId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating Customer: " + e.getMessage());
            return "redirect:/customers";
        }
    }

    @GetMapping("/details/{id}")
    public String showCustomerDetails(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Displaying details for customer ID (encrypted): {}", id);
        try {
            // Decrypt and validate customer ID
            Long customerId = validation.decryptAndValidateId(id);
            if (customerId == null) {
                logger.warn("Invalid customer ID provided: {}", id);
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid customer ID.");
                return "redirect:/customers";
            }
            logger.debug("Decrypted customer ID: {}", customerId);
            // Fetch customer details
            Customer customer = customerService.getCustomerById(customerId);
            if (customer == null) {
                logger.warn("Customer not found with ID: {}", customerId);
                redirectAttributes.addFlashAttribute("errorMessage", "Customer not found.");
                return "redirect:/customers";
            }
            model.addAttribute("customer", customer);
            logger.info("Fetched customer details: {}", customer);
            // Fetch customer measurements
            List<CustomerMeasurement> measurements = customer.getMeasurements();
            model.addAttribute("measurements", measurements);
            logger.info("Fetched measurement details: {}", measurements);
            // Fetch product details (orders/products associated with the customer)
            // Fetch products from measurements
            List<Product> productsList = new ArrayList<>();
            if (measurements != null) {
                for (CustomerMeasurement measurement : measurements) {
                    if (measurement.getProduct() != null) {
                        productsList.add(measurement.getProduct());
                    }
                }
            }

            // customer ledger
            Map<String, List<CustomerPaymentLedger>> orderLedgerMap = new LinkedHashMap<>();
            Map<String, BigDecimal> orderBalances = new HashMap<>();

            // Initialize total variables for credits and debits
            BigDecimal totalCredit = BigDecimal.ZERO;
            BigDecimal totalDebit = BigDecimal.ZERO;

            List<CustomerPaymentLedger> ledgerEntries = customerPaymentLedgerService.findByCustomerIdOrderByOrderIdAscPaymentDateAsc(customerId);

            for (CustomerPaymentLedger payment : ledgerEntries) {
                String orderId = payment.getOrder().getOrderId();
                orderLedgerMap.computeIfAbsent(orderId, k -> new ArrayList<>()).add(payment);

                // Calculate balance
                BigDecimal balance = orderBalances.getOrDefault(orderId, BigDecimal.ZERO);
                if (payment.getPaymentType() == PaymentType.CREDIT) {
                    balance = balance.add(payment.getAmount());
                    totalCredit = totalCredit.add(payment.getAmount());  // Add to total credit
                } else {
                    balance = balance.subtract(payment.getAmount());
                    totalDebit = totalDebit.add(payment.getAmount());  // Add to total debit
                }
                orderBalances.put(orderId, balance);
            }
            model.addAttribute("totalCredit", totalCredit);
            model.addAttribute("totalDebit", totalDebit);
            model.addAttribute("orderLedgerMap", orderLedgerMap);
            model.addAttribute("orderBalances", orderBalances);

            model.addAttribute("products", productsList);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());

        } catch (Exception e) {
            logger.error("Error loading customer details for ID (encrypted) {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error loading customer details: " + e.getMessage());
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
