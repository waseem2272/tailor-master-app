package com.example.tailormaster.validation.products;

import com.example.tailormaster.entity.Product;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

public class SelectedProductsValidator implements ConstraintValidator<ValidateSelectedProducts, List<Product>> {

    @Override
    public boolean isValid(List<Product> productIds, ConstraintValidatorContext context) {
        if (productIds == null || productIds.isEmpty()) {
            return true; // No products selected, no need for measurement validations
        } return false;
    }
}
