package com.example.tailormaster.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MeasurementFieldForm {
    private List<ProductMeasurementFieldDTO> fields = new ArrayList<>();
}