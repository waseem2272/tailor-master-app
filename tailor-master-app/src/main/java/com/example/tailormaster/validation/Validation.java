package com.example.tailormaster.validation;

import com.example.tailormaster.dto.CustomerOrderDto;
import com.example.tailormaster.entity.CustomerMeasurement;
import io.micrometer.common.util.StringUtils;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

public class Validation {

    public static void validateMeasurement(Long productId, CustomerMeasurement measurement, BindingResult result) {
        if (productId == 4L) { // Qameez
            validateField(measurement.getChest(), "customerMeasurements[" + productId + "].chest", Utility.CHEST, result);
            validateField(measurement.getSleeveLength(), "customerMeasurements[" + productId + "].sleeveLength", Utility.SLEEVE, result);
            validateField(measurement.getShoulder(), "customerMeasurements[" + productId + "].shoulder", Utility.SHOULDER, result);
        } else if (productId == 2L) { // Shalwar
            validateField(measurement.getHips(), "customerMeasurements[" + productId + "].hips", Utility.HIPS, result);
            validateField(measurement.getWaist(), "customerMeasurements[" + productId + "].waist", Utility.WAIST, result);
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

    public static boolean validateCustomerOrderDto(CustomerOrderDto orderDto) {
        if (orderDto != null) {
            return orderDto.getCustomerId() != null;
        }
        return false;
    }
}
