package com.example.tailormaster.controller;

import com.example.tailormaster.datatables.DataTablesResponse;
import com.example.tailormaster.dto.CustomerRegistrationDTO;
import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.Product;
import com.example.tailormaster.repository.product.ProductRepository;
import com.example.tailormaster.service.customer.CustomerMeasurementService;
import com.example.tailormaster.service.customer.CustomerService;
import com.example.tailormaster.service.product.ProductService;
import io.micrometer.common.util.StringUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    private CustomerService customerService;
    private ProductService productService;
    private CustomerMeasurementService measurementService;

    public CustomerController(CustomerService customerService, ProductService productService,
                              CustomerMeasurementService measurementService) {
        this.customerService = customerService;
        this.productService = productService;
        this.measurementService = measurementService;
    }

//    @GetMapping
//    @ResponseBody
//    public DataTablesResponse getCustomers(@RequestParam("draw") int draw,
//                                           @RequestParam("start") int start,
//                                           @RequestParam("length") int length,
//                                           @RequestParam("search[value]") String search) {
//        int page = start / length;
//        Page<Customer> customerPage;
//
//        if (search != null && !search.isEmpty()) {
//            customerPage = customerService.searchCustomers(search, PageRequest.of(page, length));
//        } else {
//            customerPage = customerService.getAllCustomers(PageRequest.of(page, length));
//        }
//
//        DataTablesResponse response = new DataTablesResponse();
//        response.setDraw(draw);
//        response.setRecordsTotal(customerService.countAllCustomers());
//        response.setRecordsFiltered((int) customerPage.getTotalElements());
//        response.setData(customerPage.getContent());
//
//        return response;
//    }


    // List all customers
    @GetMapping
    public String listCustomers(Model model) {
        model.addAttribute("customers", customerService.getAllCustomers());
        return "customer/list";
    }

    // Show create customer form
    @GetMapping("/create")
    public String showCreateCustomerForm(Model model) {
//        model.addAttribute("customer", new Customer());
//        model.addAttribute("products", productService.getAllActiveProducts());
//        model.addAttribute("customerMeasurement", new CustomerMeasurement());
        CustomerRegistrationDTO registrationDTO = new CustomerRegistrationDTO();
        registrationDTO.setCustomer(new Customer());
        registrationDTO.setProducts(productService.getAllActiveProducts());

        // Create a new CustomerMeasurement for each product
        for (Product product : registrationDTO.getProducts()) {
            CustomerMeasurement customerMeasurement = new CustomerMeasurement();
            registrationDTO.addCustomerMeasurement(product.getId(), customerMeasurement);
        }
//        registrationDTO.setCustomerMeasurement(new CustomerMeasurement());
        model.addAttribute("registrationDTO", registrationDTO);
        return "customer/create";
    }

    // Handle create customer form submission
//    @PostMapping("/create")
//    public String createCustomer(@Valid @ModelAttribute("customer") Customer customer,
//                                 BindingResult result,
//                                 RedirectAttributes redirectAttributes,
//                                 Model model) {
//        if (result.hasErrors()) {
//            model.addAttribute("products", productService.getAllActiveProducts());
//            return "customer/create";
//        }
//
//        try {
//            customerService.createCustomer(customer);
//            redirectAttributes.addFlashAttribute("successMessage", "Customer created successfully!");
//        } catch (RuntimeException e) {
//            redirectAttributes.addFlashAttribute("errorMessage", "Error creating customer: " + e.getMessage());
//        }
//        return "redirect:/customers";
//    }

//    @PostMapping("/create")
//    public String createCustomer(@Validated @ModelAttribute CustomerRegistrationDTO registrationDTO,
//                                 BindingResult result,
//                                 @RequestParam Long[] productIds,
//                                 RedirectAttributes redirectAttributes,
//                                 Model model) {
//
//        // Validate selected products
//        if (productIds == null || productIds.length == 0) {
//            result.rejectValue("products", "error.products", "At least one product must be selected.");
//        }
//
//        // Validate measurements based on selected products
//        for (Long productId : productIds) {
//            if (productId == 4L) { // Qameez
//                CustomerMeasurement qameezMeasurement = registrationDTO.getCustomerMeasurements().get(productId); // Assuming Qameez ID is 4
//                validateMeasurement(productId, qameezMeasurement.getChest(), "customerMeasurements[" + productId + "].chest", "Chest measurement is required for Qameez.", result);
//                validateMeasurement(productId, qameezMeasurement.getSleeveLength(), "customerMeasurements[" + productId + "].sleeveLength", "Sleeve Length is required for Qameez.", result);
//            } else if (productId == 2L) { // Shalwar
//                CustomerMeasurement shalwarMeasurement = registrationDTO.getCustomerMeasurements().get(productId); // Assuming Qameez ID is 4
//                validateMeasurement(productId, shalwarMeasurement.getHips(), "customerMeasurements[" + productId + "].hips", "Hips measurement is required for Shalwar.", result);
//                validateMeasurement(productId, shalwarMeasurement.getWaist(), "customerMeasurements[" + productId + "].waist", "Waist measurement is required for Shalwar.", result);
//            }
//        }
//
//
//        // If validation fails, return with error messages
//        if (result.hasErrors()) {
//            redirectAttributes.addFlashAttribute("errorMessage", "Customer creation failed. Please check the form and try again.");
//            model.addAttribute("registrationDTO", registrationDTO);
//            model.addAttribute("org.springframework.validation.BindingResult.registrationDTO", result); // Add BindingResult to the model
//            return "customer/create"; // Return the same view
//        }
//
//        try {
//            // Save customer details
//            Customer savedCustomer = customerService.createCustomer(registrationDTO.getCustomer());
//
//            // Save measurements for each selected product
//            for (Long productId : productIds) {
//                Product product = productService.getProductById(productId);
//                saveCustomerMeasurement(savedCustomer, product, registrationDTO.getCustomerMeasurements().get(productId));
//            }
//
//            // Success message
//            redirectAttributes.addFlashAttribute("successMessage", "Customer created successfully.");
//            return "redirect:/customers";
//
//        } catch (Exception e) {
//            // Handle unexpected errors
//            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while creating the customer.");
//            return "redirect:/customer/create";
//        }
//    }

    @PostMapping("/create")
    public String createCustomer(
            @Validated @ModelAttribute CustomerRegistrationDTO registrationDTO,
            BindingResult result,
            @RequestParam(value = "productIds", required = false) Long[] productIds,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("registrationDTO", registrationDTO);
            model.addAttribute("org.springframework.validation.BindingResult.registrationDTO", result);
            return "customer/create"; // Make sure this matches your Thymeleaf template name
        }

        // Validate selected products
        if (productIds == null || productIds.length == 0) {
            result.rejectValue("products", "error.products", "At least one product must be selected.");
            model.addAttribute("registrationDTO", registrationDTO);
            model.addAttribute("org.springframework.validation.BindingResult.registrationDTO", result);
            return "customer/create"; // Make sure this matches your Thymeleaf template name
        }

        // Validate measurements dynamically based on selected products
        Map<Long, CustomerMeasurement> customerMeasurements = registrationDTO.getCustomerMeasurements();
        for (Long productId : productIds) {
            CustomerMeasurement measurement = customerMeasurements.get(productId);

            if (measurement == null) {
                result.rejectValue("customerMeasurements", "error.customerMeasurements", "Measurements are required for the selected product.");
                continue;
            }

            validateMeasurement(productId, measurement, result);
        }

        // If validation fails, return with error messages
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Customer creation failed. Please check the form and try again.");
            model.addAttribute("registrationDTO", registrationDTO);
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
            return "redirect:/customers";

        } catch (Exception e) {
            // Handle unexpected errors
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while creating the customer.");
            return "redirect:/customer/create";
        }
    }

    /**
     * Validate measurement fields dynamically based on the selected product.
     */
    private void validateMeasurement(Long productId, CustomerMeasurement measurement, BindingResult result) {
        if (productId == 4L) { // Qameez
            validateField(measurement.getChest(), "customerMeasurements[" + productId + "].chest", "Chest measurement is required for Qameez.", result);
            validateField(measurement.getSleeveLength(), "customerMeasurements[" + productId + "].sleeveLength", "Sleeve Length is required for Qameez.", result);
        } else if (productId == 2L) { // Shalwar
            validateField(measurement.getHips(), "customerMeasurements[" + productId + "].hips", "Hips measurement is required for Shalwar.", result);
            validateField(measurement.getWaist(), "customerMeasurements[" + productId + "].waist", "Waist measurement is required for Shalwar.", result);
        }
    }

    /**
     * Helper method to validate individual measurement fields.
     */
    private void validateField(String value, String fieldPath, String errorMessage, BindingResult result) {
        if (value == null) {
            result.rejectValue(fieldPath, "error.measurement", errorMessage);
        }
    }

    // Utility method for validating individual measurement fields
    private void validateMeasurement(Long productId, String value, String field, String errorMessage, BindingResult result) {
        if (StringUtils.isBlank(value)) {
//            result.rejectValue("customerMeasurements[" + productId + "]." + field, "error." + field, errorMessage);
            result.addError(new FieldError("customerRegistrationDTO",
                    "customerMeasurements[" + productId + "]." + field,
                    errorMessage));
        }
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
        String barcode = customer.getFullName() + customer.getPhoneNumber() + "-" + UUID.randomUUID();
        measurement.setBarcode(barcode);

        measurementService.saveMeasurement(measurement);
    }


    // Show update customer form
    @GetMapping("/update/{id}")
    public String showUpdateCustomerForm(@PathVariable Long id, Model model) {
        model.addAttribute("customer", customerService.getCustomerById(id));
        return "customer/update";
    }

    // Handle update customer form submission
    @PostMapping("/update/{id}")
    public String updateCustomer(@PathVariable Long id,
                                 @Valid @ModelAttribute("customer") Customer updatedCustomer,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            // If validation fails, stay on the update page and display validation errors
            return "customer/update";
        }

        try {
            customerService.updateCustomer(id, updatedCustomer);
            redirectAttributes.addFlashAttribute("successMessage", "Customer updated successfully!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating customer: " + e.getMessage());
        }
        return "redirect:/customers";
    }

    // Delete customer
    @GetMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return "redirect:/customers";
    }
}
