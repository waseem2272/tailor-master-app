package com.example.tailormaster.entity.labor;

import com.example.tailormaster.entity.BaseEntity;
import com.example.tailormaster.enums.LaborStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@ToString
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Labor extends BaseEntity {

    @NotBlank(message = "Name is required")
    @Size(min = 3, message = "Name must have at least 3 characters")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Contact is required")
    @Pattern(regexp = "^03[0-9]{9}$", message = "Contact must be 11 digits and start with 03")
    private String contact;

    private String skills;

//    @Column(nullable = false)
    private LocalDate joinDate;

    @Enumerated(EnumType.STRING)
    private LaborStatus status = LaborStatus.ACTIVE;
}

