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

    // Step 1
    @NotBlank
    private String fullName;

    @NotBlank
    private String phoneNumber;

    private boolean enabled = true;

    // Step 2
    @NotEmpty(message = "Select at least one product")
    private List<Long> selectedProductIds = new ArrayList<>();

    /**
     * Step 3 Measurements:
     * Map<productId, Map<fieldId, value>>
     * e.g. measurements[12][101] = "40"
     */
    private Map<Long, Map<Long, String>> measurements = new HashMap<>();
}
