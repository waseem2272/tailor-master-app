package com.example.tailormaster.entity;

import com.example.tailormaster.validation.OptionalPhoneValidation;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

//    @NotBlank(message = "Username is required")
    private String username;

//    @NotBlank(message = "Password is required")
    @ToString.Exclude
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

//    private String fathersName;

    @NotBlank(message = "Phone Number 1 is required")
    @Pattern(regexp = "^03[0-9]{9}$", message = "Phone Number 1 must be 11 digits and start with 03")
    private String phone1;

    @Pattern(regexp = "^03[0-9]{9}$", message = "Phone Number 2 must be 11 digits and start with 03",
            groups = {OptionalPhoneValidation.class})
    private String phone2;

//    @DateTimeFormat(pattern = "yyyy-MM-dd")  // Ensure correct format
//    private LocalDate dateOfBirth;

    @NotBlank(message = "Shop Name is required")
    private String shopName;

    @NotBlank(message = "Proprietor Name is required")
    private String proprietorName;

    @NotBlank(message = "Shop Address is required")
    @Lob
    private String shopAddress;

//    @NotBlank(message = "Short Code is required")
    private String shortCode;

    @Lob
    @Column(name = "logo", columnDefinition = "LONGBLOB")
    @ToString.Exclude
    private byte[] logo;

    @ToString.Exclude
    private String logoContentType; // or logoPath

    private boolean enabled;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
//    @NotEmpty(message = "At least one role must be selected.") // Validation annotation
    @ToString.Exclude
    private Set<Role> roles = new HashSet<>();

    @DateTimeFormat(pattern = "yyyy-MM-dd")  // Ensure correct format
    private LocalDate trialStartedAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd")  // Ensure correct format
    private LocalDate trialEndsAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Order> orders;
}