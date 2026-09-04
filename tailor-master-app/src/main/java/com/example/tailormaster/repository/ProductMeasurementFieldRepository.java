package com.example.tailormaster.repository;

import com.example.tailormaster.entity.ProductMeasurementField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductMeasurementFieldRepository extends JpaRepository<ProductMeasurementField, Long> {

    List<ProductMeasurementField> findByProductId(Long productId);

    void deleteByProductId(Long productId);
    List<ProductMeasurementField> findByProductIdOrderByIdAsc(Long productId);
}
