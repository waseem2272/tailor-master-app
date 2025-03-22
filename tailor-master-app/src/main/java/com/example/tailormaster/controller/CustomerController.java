package com.example.tailormaster.controller;

import com.example.tailormaster.dto.CustomerDTO;
import com.example.tailormaster.dto.CustomerRegistrationDTO;
import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.service.customer.CustomerMeasurementService;
import com.example.tailormaster.service.customer.CustomerService;
import com.example.tailormaster.service.product.ProductService;
import com.example.tailormaster.util.ThymeleafUtil;
import com.example.tailormaster.validation.Utility;
import com.example.tailormaster.validation.Validation;
import io.micrometer.common.util.StringUtils;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
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

    private final CustomerService customerService;
    private final ProductService productService;
    private final CustomerMeasurementService measurementService;
    private final Validation validation;

    // List all customers
    @GetMapping
    public String listCustomers(Model model) {
        model.addAttribute("thymeleafUtil", new ThymeleafUtil());
        return "customer/list";
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
            return customerService.getCustomersData(draw, start, length, searchValue, columnIndex, sortDirection, startDate, endDate);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving customers from the database.", e);
        }
    }

    // Show create customer form
    @GetMapping("/create")
    public String showCreateCustomerForm(Model model) {
        model.addAttribute("customer", new Customer());
        CustomerRegistrationDTO registrationDTO = populateCustomerRegistrationDTO();
        model.addAttribute("registrationDTO", registrationDTO);
        return "customer/create";
    }

    // create customer
    @PostMapping("/create")
    public String createCustomer(
            @Valid @ModelAttribute CustomerRegistrationDTO registrationDTO,
            BindingResult result,
            @RequestParam(value = "productIds", required = false) Long[] productIds,
            RedirectAttributes redirectAttributes,
            Model model) {

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

        // If validation fails, return with error messages
        if (result.hasErrors()) {
            CustomerRegistrationDTO tempRegistrationDTO = populateCustomerRegistrationDTO(registrationDTO);

            // Extract selected product IDs
            Set<Long> selectedProductIds = productIds != null ? Set.of(productIds) : new HashSet<>();
            tempRegistrationDTO.setSelectedProductIds(selectedProductIds);
            model.addAttribute("registrationDTO", tempRegistrationDTO);
            model.addAttribute("org.springframework.validation.BindingResult.registrationDTO", result);
            return "customer/create";
        }

        try {
            // Save customer details
            Customer savedCustomer = customerService.createCustomer(registrationDTO.getCustomer());

            // Save measurements for each selected product
            for (Long productId : productIds) {
                Product product = productService.getProductById(productId);
                saveCustomerMeasurement(savedCustomer, product, customerMeasurements.get(productId));
            }

            // Success message
            redirectAttributes.addFlashAttribute("successMessage", "Customer created successfully.");
            redirectAttributes.addFlashAttribute("customerId", savedCustomer.getId());
            return "redirect:/customers";

        } catch (Exception e) {
            // Handle unexpected errors
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
    }

    private CustomerRegistrationDTO populateCustomerRegistrationDTO() {
        CustomerRegistrationDTO registrationDTO = new CustomerRegistrationDTO();
        registrationDTO.setProducts(productService.getAllActiveProducts());

        // Create a new CustomerMeasurement for each product
        for (Product product : registrationDTO.getProducts()) {
            CustomerMeasurement customerMeasurement = new CustomerMeasurement();
            registrationDTO.addCustomerMeasurement(product.getId(), customerMeasurement);
        }
        return registrationDTO;
    }

    private CustomerRegistrationDTO populateCustomerRegistrationDTO(CustomerRegistrationDTO existingDTO) {
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
    }

    @GetMapping("/edit/{id}")
    public String showEditCustomerForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        try {
            // Decrypt and validate customer ID
            Long customerId = validation.validateAndFetchCustomer(id, redirectAttributes);
            if (customerId == null) {
                return "redirect:/customers";
            }

            Customer customer = customerService.getCustomerById(customerId);

            CustomerRegistrationDTO registrationDTO = populateEditCustomerRegistrationDTO(customer);
            model.addAttribute("registrationDTO", registrationDTO);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());
            return "customer/update";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating Customer: " + e.getMessage());
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

        try {
            // Decrypt and validate customer ID
            Long customerId = validation.validateAndFetchCustomer(encryptedCustomerId, redirectAttributes);
            if (customerId == null) {
                return "redirect:/customers";
            }

            // Validate selected products
            if (productIds == null || productIds.length == 0) {
                result.rejectValue("products", Utility.PRODUCT_ERROR_CODE, Utility.PRODUCT_ERROR_MESSAGE);
            }

            List<Product> selectedProducts = new ArrayList<>();

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
                        selectedProducts.add(product);
                    }
                }
            }

            // set decrypted customer id
            registrationDTO.getCustomer().setId(customerId);

            // If validation fails, return with error messages
            if (result.hasErrors()) {
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
            redirectAttributes.addFlashAttribute("successMessage", "Customer updated successfully!");
            redirectAttributes.addFlashAttribute("customerId", updatedCustomer.getId());
            return "redirect:/customers";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating Customer: " + e.getMessage());
            return "redirect:/customers";
        }
    }

    @GetMapping("/details/{id}")
    public String showCustomerDetails(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {

        try {
            // Decrypt and validate customer ID
            Long customerId = validation.decryptAndValidateId(id);
            if (customerId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid customer ID.");
                return "redirect:/customers";
            }

            // Fetch customer details
            Customer customer = customerService.getCustomerById(customerId);
            model.addAttribute("customer", customer);

            // Fetch customer measurements
            List<CustomerMeasurement> measurements = customer.getMeasurements();
            model.addAttribute("measurements", measurements);

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
            model.addAttribute("products", productsList);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());

        } catch (Exception e) {
            e.printStackTrace();
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
