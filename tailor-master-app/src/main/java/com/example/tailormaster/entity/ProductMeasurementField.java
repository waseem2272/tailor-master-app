package com.example.tailormaster.entity;

import com.example.tailormaster.entity.product.Product;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "product_measurement_field")
public class ProductMeasurementField extends BaseEntity {

    public ProductMeasurementField(Long id) {super(id);}

    private String fieldName;
    private String fieldType;
    private String options;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
