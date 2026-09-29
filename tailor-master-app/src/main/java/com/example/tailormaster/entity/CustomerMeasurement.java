package com.example.tailormaster.entity;

import com.example.tailormaster.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @ToString
@Entity
public class CustomerMeasurement extends BaseEntity {

    private String value;   // e.g., "40", "15.5", "Slim Fit"

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "field_id")
    private ProductMeasurementField field;

    @ManyToOne
    @JoinColumn(name = "customer_product_measurement_id")
    private CustomerProductMeasurement customerProductMeasurement;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
