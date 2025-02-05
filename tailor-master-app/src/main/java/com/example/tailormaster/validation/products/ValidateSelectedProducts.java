package com.example.tailormaster.validation.products;

import com.example.tailormaster.validation.MeasurementValidationGroup;
import com.example.tailormaster.validation.ProductSelectionGroup;
import jakarta.validation.Constraint;
import jakarta.validation.GroupSequence;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SelectedProductsValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@GroupSequence({ProductSelectionGroup.class, MeasurementValidationGroup.class})
public @interface ValidateSelectedProducts {
    String message() default "At least one product must be selected.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
