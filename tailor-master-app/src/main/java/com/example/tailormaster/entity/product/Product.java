package com.example.tailormaster.entity.product;

import com.example.tailormaster.entity.BaseEntity;
import com.example.tailormaster.entity.CustomerMeasurement;
import com.example.tailormaster.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Product extends BaseEntity {

    public Product(Long id) {
        super(id);
    }
    private String name;
    private BigDecimal singleSilai;
    private BigDecimal doubleSilai;
    @Column(nullable = false)
    private boolean enabled = true;
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<CustomerMeasurement> measurements;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
