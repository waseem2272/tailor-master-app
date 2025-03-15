package com.example.tailormaster.dto;

import lombok.Data;

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