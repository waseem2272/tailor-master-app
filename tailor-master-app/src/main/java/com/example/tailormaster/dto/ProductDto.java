package com.example.tailormaster.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;

    public ProductDto(Long id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }
}
