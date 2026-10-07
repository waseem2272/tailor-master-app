package com.example.tailormaster.entity;

import com.example.tailormaster.entity.labor.Labor;
import com.example.tailormaster.entity.product.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
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

    private String username;

    @ToString.Exclude
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone Number 1 is required")
    private String phone1;

    private String phone2;

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

    private boolean inventoryEnabled = true;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @ToString.Exclude
    private Set<Role> roles = new HashSet<>();

    @DateTimeFormat(pattern = "yyyy-MM-dd")  // Ensure correct format
    private LocalDate trialStartedAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd")  // Ensure correct format
    private LocalDate trialEndsAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Order> orders;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Product> products;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Customer> customers;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Labor> labors;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private UserBackupSettings backupSettings;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<BackupHistory> backupHistories;
}