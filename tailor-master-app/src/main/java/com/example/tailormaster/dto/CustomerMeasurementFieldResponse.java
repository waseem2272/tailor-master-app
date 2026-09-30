package com.example.tailormaster.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerMeasurementFieldResponse {
    private Long fieldId;
    private String fieldName;
    private String fieldType;
    private String value;
    private String options;
}