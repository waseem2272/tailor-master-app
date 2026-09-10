package com.example.tailormaster.service;

import com.example.tailormaster.entity.InventoryBrand;
import com.example.tailormaster.repository.InventoryBrandRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryBrandService {

    private final InventoryBrandRepository inventoryBrandRepository;

    @Transactional
    public List<InventoryBrand> getAllActive() {
        return inventoryBrandRepository.findAll()
                .stream()
                .filter(InventoryBrand::getActive)
                .toList();
    }

    @Transactional
    public List<InventoryBrand> getAll() {
        return inventoryBrandRepository.findAll();
    }

    @Transactional
    public InventoryBrand getById(Long id) {
        return inventoryBrandRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Brand not found with id: " + id));
    }

    public InventoryBrand save(InventoryBrand brand) {

        if (brand.getName() == null || brand.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Brand name is required");
        }

        String name = brand.getName().trim();

        if (brand.getId() == null &&
                inventoryBrandRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Brand already exists");
        }

        brand.setName(name);

        if (brand.getActive() == null) {
            brand.setActive(true);
        }

        return inventoryBrandRepository.save(brand);
    }

    public void delete(Long id) {

        InventoryBrand brand = getById(id);

        // Soft delete
        brand.setActive(false);

        inventoryBrandRepository.save(brand);
    }
}