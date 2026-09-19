package com.example.tailormaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// CustomerWizardDTO.java
@Data
public class CustomerWizardDTO {
    @NotBlank
    private String fullName;

    @NotBlank
    private String phoneNumber;

    private boolean enabled = true;

    private boolean addTailoringMeasurements = true;

    private List<Long> selectedProductIds = new ArrayList<>();

    private Map<Long, Map<Long, String>> measurements = new HashMap<>();
}