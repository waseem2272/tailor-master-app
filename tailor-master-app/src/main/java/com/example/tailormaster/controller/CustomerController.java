package com.example.tailormaster.controller;

import com.example.tailormaster.dto.CustomerRegistrationDTO;
import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.service.customer.CustomerMeasurementService;
import com.example.tailormaster.service.customer.CustomerService;
import com.example.tailormaster.service.product.ProductService;
import com.example.tailormaster.validation.Utility;
import io.micrometer.common.util.StringUtils;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

import static com.example.tailormaster.validation.Validation.validateMeasurement;

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
        model.addAttribute("customer", new Customer());
        CustomerRegistrationDTO registrationDTO = populateCustomerRegistrationDTO();
        model.addAttribute("registrationDTO", registrationDTO);
        return "customer/create";
    }

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
        if (productIds != null) {
            for (Long productId : productIds) {
                CustomerMeasurement measurement = customerMeasurements.get(productId);
                if (measurement == null) {
                    result.rejectValue("customerMeasurements", "error.measurements", "Measurements are required for the selected product.");
                } else {
                    validateMeasurement(productId, measurement, result);
                }
            }
        }

        // If validation fails, return with error messages
        if (result.hasErrors()) {
            CustomerRegistrationDTO tempRegistrationDTO = populateCustomerRegistrationDTO(registrationDTO);

            // Extract selected product IDs
            List<Long> selectedProductIds = productIds != null ? Arrays.asList(productIds) : new ArrayList<>();

            model.addAttribute("selectedProductIds", selectedProductIds);
            model.addAttribute("registrationDTO", tempRegistrationDTO);
            model.addAttribute("org.springframework.validation.BindingResult.registrationDTO", result);
            return "customer/create";
        }

        // Add the registrationDTO to the model to display on the preview page
        model.addAttribute("registrationDTO", registrationDTO);
        return "customer/preview";  // Show the preview page

//        try {
//            // Save customer details
//            Customer savedCustomer = customerService.createCustomer(registrationDTO.getCustomer());
//
//            // Save measurements for each selected product
//            for (Long productId : productIds) {
//                Product product = productService.getProductById(productId);
//                saveCustomerMeasurement(savedCustomer, product, customerMeasurements.get(productId));
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

}
