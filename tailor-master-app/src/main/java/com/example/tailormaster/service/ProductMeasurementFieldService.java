package com.example.tailormaster.service;

import com.example.tailormaster.dto.ProductMeasurementFieldDTO;
import com.example.tailormaster.entity.ProductMeasurementField;
import com.example.tailormaster.entity.product.Product;
import com.example.tailormaster.repository.ProductMeasurementFieldRepository;
import com.example.tailormaster.repository.product.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductMeasurementFieldService {
    private final ProductMeasurementFieldRepository productMeasurementFieldRepository;
    private final ProductRepository productRepository;

    public List<ProductMeasurementField> getMeasurementFieldsByProductId(Long productId) {
        return productMeasurementFieldRepository.findByProductId(productId);

    }

    @Transactional
    public void saveFields(Long productId, List<ProductMeasurementFieldDTO> dtos) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // fetch existing fields
        List<ProductMeasurementField> existingFields =
                productMeasurementFieldRepository.findByProductId(productId);

        Map<Long, ProductMeasurementField> existingFieldMap = existingFields.stream()
                .collect(Collectors.toMap(ProductMeasurementField::getId, f -> f));

        List<ProductMeasurementField> fieldsToSave = new ArrayList<>();

        for (ProductMeasurementFieldDTO dto : dtos) {
            if (dto.getId() != null && existingFieldMap.containsKey(dto.getId())) {
                // update existing field
                ProductMeasurementField field = existingFieldMap.get(dto.getId());
                field.setFieldName(dto.getFieldName());
                field.setFieldType(dto.getFieldType());
                field.setOptions(dto.getOptions() != null ? String.join(",", dto.getOptions()) : null);
                fieldsToSave.add(field);
            } else {
                // create new field
                ProductMeasurementField newField = new ProductMeasurementField();
                newField.setFieldName(dto.getFieldName());
                newField.setFieldType(dto.getFieldType());
                newField.setOptions(dto.getOptions() != null ? String.join(",", dto.getOptions()) : null);
                newField.setProduct(product);
                fieldsToSave.add(newField);
            }
        }

        productMeasurementFieldRepository.saveAll(fieldsToSave);

        // optional: handle old fields that are no longer in DTO
        // instead of delete -> mark inactive (add a status column)
    }


}
