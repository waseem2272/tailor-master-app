package com.example.tailormaster.dto;

import com.example.tailormaster.entity.Role;
import com.example.tailormaster.validation.OptionalPhoneValidation;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RegisterUserForm {

    private String username;
    private String password;
    private String fullName;
    private String fathersName;
    private String phone1;
    private String phone2;
    private LocalDate dateOfBirth;
    private String shopName;
    private String proprietorName;
    private String shopAddress;
    private String shortCode;
    private String role;
}