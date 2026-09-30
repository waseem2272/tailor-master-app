package com.example.tailormaster.service;

import com.example.tailormaster.dto.ProductMeasurementFieldDTO;
import com.example.tailormaster.entity.ProductMeasurementField;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.repository.ProductMeasurementFieldRepository;
import com.example.tailormaster.repository.product.ProductRepository;
import com.example.tailormaster.util.AuthenticatedUserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductMeasurementFieldService {
    private final ProductMeasurementFieldRepository productMeasurementFieldRepository;
    private final ProductRepository productRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public Long getCurrentUserId() {
        return authenticatedUserService.getCurrentUser().getId();
    }

    public List<ProductMeasurementField> getMeasurementFieldsByProductId(Long productId) {
        return productMeasurementFieldRepository
                .findByUserIdAndProductIdAndEnabledTrue(getCurrentUserId(), productId);
    }

    @Transactional
    public void saveFields(Long productId, List<ProductMeasurementFieldDTO> dtos) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        List<ProductMeasurementField> existingFields =
                productMeasurementFieldRepository.findByUserIdAndProductId(getCurrentUserId(), productId);

        Map<Long, ProductMeasurementField> existingFieldMap = existingFields.stream()
                .collect(Collectors.toMap(ProductMeasurementField::getId, f -> f));

        Set<Long> submittedFieldIds = new HashSet<>();

        List<ProductMeasurementField> fieldsToSave = new ArrayList<>();

        if (dtos != null) {
            for (ProductMeasurementFieldDTO dto : dtos) {

                if (dto.getFieldName() == null || dto.getFieldName().isBlank()) {
                    continue;
                }

                if (dto.getId() != null && existingFieldMap.containsKey(dto.getId())) {

                    ProductMeasurementField field = existingFieldMap.get(dto.getId());

                    field.setFieldName(dto.getFieldName().trim());
                    field.setFieldType(dto.getFieldType());
                    field.setEnabled(true);
                    field.setUser(authenticatedUserService.getCurrentUser());

                    if ("DROPDOWN".equals(dto.getFieldType())
                            && dto.getOptions() != null
                            && !dto.getOptions().isEmpty()) {

                        field.setOptions(String.join(",", dto.getOptions()));

                    } else {
                        field.setOptions(null);
                    }

                    submittedFieldIds.add(dto.getId());
                    fieldsToSave.add(field);

                } else {

                    ProductMeasurementField newField = new ProductMeasurementField();

                    newField.setFieldName(dto.getFieldName().trim());
                    newField.setFieldType(dto.getFieldType());
                    newField.setEnabled(true);
                    newField.setProduct(product);
                    newField.setUser(authenticatedUserService.getCurrentUser());

                    if ("DROPDOWN".equals(dto.getFieldType())
                            && dto.getOptions() != null
                            && !dto.getOptions().isEmpty()) {

                        newField.setOptions(String.join(",", dto.getOptions()));

                    } else {
                        newField.setOptions(null);
                    }

                    fieldsToSave.add(newField);
                }
            }
        }

        // Fields removed from the form are only disabled.
        // They are NOT physically deleted because CustomerMeasurement
        // may still reference them.
        for (ProductMeasurementField field : existingFields) {

            if (!submittedFieldIds.contains(field.getId())) {
                field.setEnabled(false);
                fieldsToSave.add(field);
            }
        }

        productMeasurementFieldRepository.saveAll(fieldsToSave);
    }


}
