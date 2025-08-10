package com.example.tailormaster.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@ToString
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
    @ToString.Exclude
    private List<CustomerMeasurement> measurements;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
