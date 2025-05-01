package com.example.tailormaster.entity.product;

import com.example.tailormaster.entity.BaseEntity;
import com.example.tailormaster.entity.CustomerMeasurement;
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

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('QAMEEZ', 'SHALWAR')")
//    @ValidProductType
    private ProductType type;

    @NotBlank(message = "Product Name is required")
    @Size(max = 100, message = "Product name cannot exceed 100 characters.")
    private String name;

    @Size(max = 255, message = "Description cannot exceed 255 characters.")
    private String description;

    @NotNull(message = "Price cannot be null")
    @Positive(message = "Price must be greater than 0.")
    private BigDecimal price;

    private boolean enabled;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<CustomerMeasurement> measurements;

//    @ManyToOne
//    @JoinColumn(name = "category_id")
//    private ProductCategory category;
}
