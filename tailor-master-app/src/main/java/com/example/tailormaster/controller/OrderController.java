package com.example.tailormaster.controller;

import com.example.tailormaster.dto.CustomerOrderDto;
import com.example.tailormaster.dto.OrderProductDto;
import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.OrderProduct;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.service.customer.CustomerMeasurementService;
import com.example.tailormaster.service.customer.CustomerService;
import com.example.tailormaster.service.order.OrderService;
import com.example.tailormaster.service.product.ProductService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Controller
@RequestMapping("/orders")
public class OrderController {

    private final CustomerService customerService;
    private final ProductService productService;
    private final CustomerMeasurementService customerMeasurementService;
    private final OrderService orderService;

    // Show create order form
    @GetMapping("/create/{id}")
    public String showCreateOrderForm(@PathVariable Long id, Model model) {

        Customer customer = customerService.getCustomerById(id);
        if (customer == null) {
            return "redirect:/customers?error=CustomerNotFound";
        }

        List<CustomerMeasurement> measurements = customerMeasurementService.getCustomerMeasurement(id);
        List<Product> products = measurements.stream().map(CustomerMeasurement::getProduct).toList();

        model.addAttribute("order", new Order());
        model.addAttribute("customer", customer);
//        model.addAttribute("measurements", measurements);
        model.addAttribute("products", products);

        return "order/create";
    }

    // create order
    @PostMapping("/create")
    public String createOrder(@ModelAttribute CustomerOrderDto orderDto, RedirectAttributes redirectAttributes) {
        try {
            Order order = new Order();
            order.setOrderDate(orderDto.getOrderDate());
            order.setDeliveryDate(orderDto.getDeliveryDate());
            order.setStatus(orderDto.getStatus());
//            order.setExtraCharges(orderDto.getExtraCharges());
//            order.setExtraChargesDescription(orderDto.getExtraChargesDescription());
            order.setAdvancePayment(orderDto.getAdvancePayment());
            order.setTotalPayment(orderDto.getTotalPayment());

            // Fetch customer
            Customer customer = customerService.getCustomerById(orderDto.getCustomerId());
            order.setCustomer(customer);

            // Save Products with Quantity
            List<OrderProduct> orderProducts = new ArrayList<>();
            for (OrderProductDto opDto : orderDto.getOrderProducts()) {
                OrderProduct orderProduct = new OrderProduct();
                Product product = productService.getProductById(opDto.getProductId());
                orderProduct.setProduct(product);
                orderProduct.setQuantity(opDto.getQuantity());
                orderProduct.setSubtotal(product.getPrice().multiply(new BigDecimal(opDto.getQuantity())));
                orderProduct.setOrder(order);
                orderProducts.add(orderProduct);
            }

            order.setOrderProducts(orderProducts);
            orderService.save(order);

            redirectAttributes.addFlashAttribute("successMessage", "Order created successfully!");
            return "redirect:/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating order: " + e.getMessage());
            return "redirect:/orders/create";
        }
    }
}
