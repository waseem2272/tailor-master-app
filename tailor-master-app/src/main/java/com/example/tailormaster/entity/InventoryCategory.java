package com.example.tailormaster.entity;

import com.example.tailormaster.enums.ItemType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class InventoryCategory extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    private ItemType itemType;

    @Column(nullable = false)
    private Boolean active = true;
}