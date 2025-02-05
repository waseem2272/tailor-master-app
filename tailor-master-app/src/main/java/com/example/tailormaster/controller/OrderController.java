package com.example.tailormaster.controller;

import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.Product;
import com.example.tailormaster.service.customer.CustomerService;
import com.example.tailormaster.service.product.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private CustomerService customerService;
    private ProductService productService;

    public OrderController(CustomerService customerService, ProductService productService) {
        this.customerService = customerService;
        this.productService = productService;
    }

    // List all customers
//    @GetMapping
//    public String listCustomers(Model model) {
//        model.addAttribute("customers", customerService.getAllCustomers());
//        return "customer/list";
//    }

    // Show create order form
    @GetMapping("/create/{id}")
    public String showCreateOrderForm(@PathVariable Long id, Model model) {
        List<String> productOptions = productService.getAllActiveProducts().stream()
                .map(product -> String.format("<option value=\"%d\">%s</option>", product.getId(), product.getName()))
                .collect(Collectors.toList());
        model.addAttribute("productOptions", productOptions);
        System.out.println("Products: " + productOptions);
        model.addAttribute("order", new Order());
        model.addAttribute("customer", customerService.getCustomerById(id));
        model.addAttribute("products", productService.getAllActiveProducts());

        return "order/create";
    }

    // Handle create customer form submission
//    @PostMapping("/create")
//    public String createCustomer(@Valid @ModelAttribute("customer") Customer customer,
//                                 BindingResult result,
//                                 RedirectAttributes redirectAttributes) {
//        if (result.hasErrors()) {
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

    // Show update customer form
//    @GetMapping("/update/{id}")
//    public String showUpdateCustomerForm(@PathVariable Long id, Model model) {
//        model.addAttribute("customer", customerService.getCustomerById(id));
//        return "customer/update";
//    }

    // Handle update customer form submission
//    @PostMapping("/update/{id}")
//    public String updateCustomer(@PathVariable Long id,
//                                 @Valid @ModelAttribute("customer") Customer updatedCustomer,
//                                 BindingResult bindingResult,
//                                 RedirectAttributes redirectAttributes) {
//
//        if (bindingResult.hasErrors()) {
//            // If validation fails, stay on the update page and display validation errors
//            return "customer/update";
//        }
//
//        try {
//            customerService.updateCustomer(id, updatedCustomer);
//            redirectAttributes.addFlashAttribute("successMessage", "Customer updated successfully!");
//        } catch (RuntimeException e) {
//            redirectAttributes.addFlashAttribute("errorMessage", "Error updating customer: " + e.getMessage());
//        }
//        return "redirect:/customers";
//    }

    // Delete customer
//    @GetMapping("/delete/{id}")
//    public String deleteCustomer(@PathVariable Long id) {
//        customerService.deleteCustomer(id);
//        return "redirect:/customers";
//    }
}
