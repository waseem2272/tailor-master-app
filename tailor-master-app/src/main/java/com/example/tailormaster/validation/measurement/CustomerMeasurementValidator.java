package com.example.tailormaster.validation.measurement;

import com.example.tailormaster.entity.CustomerMeasurement;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import io.micrometer.common.util.StringUtils;

import java.util.Map;

public class CustomerMeasurementValidator implements ConstraintValidator<ValidCustomerMeasurement, Map<Long, CustomerMeasurement>> {

    @Override
    public boolean isValid(Map<Long, CustomerMeasurement> customerMeasurements, ConstraintValidatorContext context) {
        if (customerMeasurements == null || customerMeasurements.isEmpty()) {
            return true; // No validation needed if no measurements are provided.
        }

        for (Map.Entry<Long, CustomerMeasurement> entry : customerMeasurements.entrySet()) {
            Long productId = entry.getKey();
            CustomerMeasurement measurement = entry.getValue();

            if (!isValidMeasurementForProduct(productId, measurement, context)) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidMeasurementForProduct(Long productId, CustomerMeasurement measurement, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        // Example: Validation rules based on productId
        if (productId == 2L) { // Shalwar
            if (StringUtils.isBlank(measurement.getWaist()) || StringUtils.isBlank(measurement.getHips())) {
                context.buildConstraintViolationWithTemplate("Waist and Hips are required for Shalwar")
                        .addConstraintViolation();
                return false;
            }
        } else if (productId == 4L) { // Qameez
            if (StringUtils.isBlank(measurement.getChest())
                    || StringUtils.isBlank(measurement.getShoulder())
                    || StringUtils.isBlank(measurement.getSleeveLength())) {
                context.buildConstraintViolationWithTemplate("Chest and Shoulder are required for Qameez")
                        .addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
