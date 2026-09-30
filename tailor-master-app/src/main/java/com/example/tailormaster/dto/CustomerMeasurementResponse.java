package com.example.tailormaster.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerMeasurementResponse {
    private Long id;
    private Long customerId;
    private Long productId;
    private String notes;
    private List<CustomerMeasurementFieldResponse> measurements;
}