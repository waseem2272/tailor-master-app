package com.example.tailormaster.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter @Setter @ToString
public class ProductDto {
    private Long id;
    private String name;
    private BigDecimal singleSilai;
    private BigDecimal doubleSilai;

    public ProductDto(Long id, String name, BigDecimal singleSilai, BigDecimal doubleSilai) {
        this.id = id;
        this.name = name;
        this.singleSilai = singleSilai;
        this.doubleSilai = doubleSilai;
    }
}
