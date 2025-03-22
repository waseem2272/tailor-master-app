package com.example.tailormaster.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Customer extends BaseEntity {
    @NotBlank(message = "Full Name is required")
    @Size(min = 3, message = "Full Name must have at least 3 characters")
    private String fullName;

//    private String fatherName;

//    private String surname;
    @NotBlank(message = "Phone Number is required")
    @Pattern(regexp = "^03[0-9]{9}$", message = "Phone Number must be 11 digits and start with 03")
    private String phoneNumber;

    private boolean enabled;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CustomerMeasurement> measurements;
}
