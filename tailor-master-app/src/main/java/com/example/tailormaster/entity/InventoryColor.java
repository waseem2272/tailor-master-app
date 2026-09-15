package com.example.tailormaster.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_color")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class InventoryColor extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "hex_code", length = 7)
    private String hexCode;

    @Column(nullable = false)
    private Boolean active = true;
}