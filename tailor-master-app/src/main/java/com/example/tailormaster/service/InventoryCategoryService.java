package com.example.tailormaster.service;

import com.example.tailormaster.entity.InventoryCategory;
import com.example.tailormaster.enums.ItemType;
import com.example.tailormaster.repository.InventoryCategoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class InventoryCategoryService {

    private final InventoryCategoryRepository inventoryCategoryRepository;

    public List<InventoryCategory> getAllActive() {
        return inventoryCategoryRepository.findByActiveTrue();
    }

    public List<InventoryCategory> getByItemType(ItemType itemType) {
        return inventoryCategoryRepository.findByItemTypeAndActiveTrue(itemType);
    }

    public InventoryCategory getById(Long id) {
        return inventoryCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }

    public InventoryCategory save(InventoryCategory category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required");
        }
        if (category.getItemType() == null) {
            throw new IllegalArgumentException("Item type is required");
        }

        String name = category.getName().trim();

        if (category.getId() == null &&
                inventoryCategoryRepository.existsByNameIgnoreCaseAndItemType(name, category.getItemType())) {
            throw new IllegalArgumentException("Category already exists for this item type");
        }

        category.setName(name);
        if (category.getActive() == null) {
            category.setActive(true);
        }

        InventoryCategory savedCategory = inventoryCategoryRepository.save(category);
        log.info("Inventory category saved successfully. Category ID: {}, Name: {}, Type: {}",
                savedCategory.getId(), savedCategory.getName(), savedCategory.getItemType());
        return savedCategory;
    }

    public void delete(Long id) {
        InventoryCategory category = getById(id);
        category.setActive(false);
        inventoryCategoryRepository.save(category);
        log.info("Inventory category soft deleted. Category ID: {}", id);
    }
}