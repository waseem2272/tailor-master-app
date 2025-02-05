package com.example.tailormaster.validation.measurement;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CustomerMeasurementValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCustomerMeasurement {
    String message() default "Invalid customer measurements for selected products";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
