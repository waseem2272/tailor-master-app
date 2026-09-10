package com.example.tailormaster.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_brand")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class InventoryBrand extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false)
    private Boolean active = true;
}