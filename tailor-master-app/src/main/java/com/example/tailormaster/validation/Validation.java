package com.example.tailormaster.validation;

import com.example.tailormaster.entity.Customer;
import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.Order;
import com.example.tailormaster.entity.labor.Labor;
import com.example.tailormaster.entity.labor.LaborPayment;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.entity.product.ProductType;
import com.example.tailormaster.service.customer.CustomerService;
import com.example.tailormaster.service.labor.LaborPaymentService;
import com.example.tailormaster.service.labor.LaborService;
import com.example.tailormaster.service.order.OrderService;
import com.example.tailormaster.util.AESUtil;
import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@AllArgsConstructor
@Service
public class Validation {

    private final CustomerService customerService;
    private final OrderService orderService;
    private final LaborService laborService;
    private final LaborPaymentService laborPaymentService;

    public void validateMeasurement(Product product, CustomerMeasurement measurement, BindingResult result) {
        if (product.getType() == ProductType.QAMEEZ) { // Qameez
            validateField(measurement.getChest(), "customerMeasurements[" + product.getId() + "].chest", Utility.CHEST, result);
            validateField(measurement.getSleeveLength(), "customerMeasurements[" + product.getId() + "].sleeveLength", Utility.SLEEVE, result);
            validateField(measurement.getShoulder(), "customerMeasurements[" + product.getId() + "].shoulder", Utility.SHOULDER, result);
        } else if (product.getType() == ProductType.SHALWAR) { // Shalwar
            validateField(measurement.getHips(), "customerMeasurements[" + product.getId() + "].hips", Utility.HIPS, result);
            validateField(measurement.getWaist(), "customerMeasurements[" + product.getId() + "].waist", Utility.WAIST, result);
        }
    }

    /**
     * Helper method to validate individual measurement fields.
     */
    public static void validateField(String value, String fieldPath, String errorMessage, BindingResult result) {
        if (StringUtils.isBlank(value)) {
            result.rejectValue(fieldPath, "error.measurement", errorMessage);
        }
    }

    public Long decryptAndValidateId(String encryptedId) {
        try {
            String decryptedId = AESUtil.decrypt(encryptedId);
            if (!decryptedId.matches("\\d+")) {
                return null;
            }
            return Long.parseLong(decryptedId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Long validateAndFetchCustomer(String encryptedCustomerId, RedirectAttributes redirectAttributes) {
        Long customerId = decryptAndValidateId(encryptedCustomerId);
        if (customerId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid customer ID.");
            return null;
        }

        Customer customer = customerService.getCustomerById(customerId);
        if (customer == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Customer not found.");
            return null;
        }

        return customerId;
    }

    public Long validateAndFetchOrder(String encryptedOrderId, RedirectAttributes redirectAttributes) {
        Long orderId = decryptAndValidateId(encryptedOrderId);
        if (orderId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid order ID.");
            return null;
        }

        Order order = orderService.findById(orderId);
        if (order == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Order not found.");
            return null;
        }

        return orderId;
    }

    public Long validateAndFetchOrder(String encryptedOrderId) {
        Long orderId = decryptAndValidateId(encryptedOrderId);
        if (orderId == null) {
            return null;
        }

        Order order = orderService.findById(orderId);
        if (order == null) {
            return null;
        }

        return orderId;
    }

    public Long validateAndFetchLabor(String encryptedLaborId, RedirectAttributes redirectAttributes) {
        Long laborId = decryptAndValidateId(encryptedLaborId);
        if (laborId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid Labor ID.");
            return null;
        }

        Optional<Labor> labor = laborService.getLaborById(laborId);
        if (labor.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Labor not found.");
            return null;
        }

        return laborId;
    }

    public Long validateAndFetchLaborPayment(String encryptedLaborPaymentId, RedirectAttributes redirectAttributes) {
        Long laborPaymentId = decryptAndValidateId(encryptedLaborPaymentId);
        if (laborPaymentId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid Labor Payment ID.");
            return null;
        }

        Optional<LaborPayment> laborPayment = laborPaymentService.getLaborPayment(laborPaymentId);
        if (laborPayment.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Labor Payment not found.");
            return null;
        }

        return laborPaymentId;
    }

}
