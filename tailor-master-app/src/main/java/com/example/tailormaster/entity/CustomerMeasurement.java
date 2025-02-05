package com.example.tailormaster.entity;

import com.example.tailormaster.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @ToString
@Entity
public class CustomerMeasurement extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private String chest;
    private String sleeveLength;
    private String shoulder;
    private String hips;
    private String waist;

    private String barcode;

    public void generateBarcode() {
        this.barcode = customer.getFullName() + customer.getPhoneNumber() + "-" + UUID.randomUUID();
    }

    // Constructors, Getters, and Setters
}
