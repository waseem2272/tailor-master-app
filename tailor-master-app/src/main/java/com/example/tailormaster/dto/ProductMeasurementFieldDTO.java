package com.example.tailormaster.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductMeasurementFieldDTO {
    private Long id;
    private String fieldName;
    private String fieldType;   // NUMBER, TEXT, DROPDOWN
    private List<String> options = new ArrayList<>();
}