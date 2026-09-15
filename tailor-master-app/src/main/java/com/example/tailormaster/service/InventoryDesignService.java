package com.example.tailormaster.service;

import com.example.tailormaster.entity.InventoryDesign;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.repository.InventoryDesignRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryDesignService {

    private final InventoryDesignRepository inventoryDesignRepository;

    public List<InventoryDesign> getActiveDesigns(User user) {
        return inventoryDesignRepository.findByUserAndActiveTrueOrderByNameAsc(user);
    }

    public Optional<InventoryDesign> findById(User user, Long id) {
        return inventoryDesignRepository.findByUserAndId(user, id);
    }

    @Transactional
    public InventoryDesign save(User user, InventoryDesign design) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User is required");
            }
            if (design == null) {
                throw new IllegalArgumentException("Design is required");
            }
            if (design.getName() == null || design.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Design name is required");
            }

            String name = design.getName().trim();

            boolean duplicate = inventoryDesignRepository.findByUserAndNameIgnoreCase(user, name)
                    .filter(existing -> design.getId() == null || !existing.getId().equals(design.getId()))
                    .isPresent();

            if (duplicate) {
                throw new IllegalArgumentException("Design already exists: " + name);
            }

            InventoryDesign designToSave;

            if (design.getId() == null) {
                design.setUser(user);
                design.setName(name);
                design.setActive(true);
                designToSave = design;
            } else {
                designToSave = inventoryDesignRepository.findByUserAndId(user, design.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Design not found"));

                designToSave.setName(name);
            }

            InventoryDesign savedDesign = inventoryDesignRepository.save(designToSave);

            log.info("Inventory design saved successfully. User ID: {}, Design ID: {}, Name: {}",
                    user.getId(), savedDesign.getId(), savedDesign.getName());

            return savedDesign;

        } catch (IllegalArgumentException e) {
            log.warn("Invalid inventory design data. User ID: {}, Message: {}",
                    user != null ? user.getId() : null, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while saving inventory design. User ID: {}",
                    user != null ? user.getId() : null, e);
            throw new RuntimeException("Unable to save inventory design", e);
        }
    }

    @Transactional
    public void deactivate(User user, Long id) {
        try {
            InventoryDesign design = inventoryDesignRepository.findByUserAndId(user, id)
                    .orElseThrow(() -> new IllegalArgumentException("Design not found"));

            design.setActive(false);
            inventoryDesignRepository.save(design);

            log.info("Inventory design deactivated. User ID: {}, Design ID: {}", user.getId(), id);

        } catch (IllegalArgumentException e) {
            log.warn("Unable to deactivate inventory design. User ID: {}, Design ID: {}, Message: {}",
                    user != null ? user.getId() : null, id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while deactivating inventory design. User ID: {}, Design ID: {}",
                    user != null ? user.getId() : null, id, e);
            throw new RuntimeException("Unable to deactivate inventory design", e);
        }
    }

    @Transactional
    public void activate(User user, Long id) {
        try {
            InventoryDesign design = inventoryDesignRepository.findByUserAndId(user, id)
                    .orElseThrow(() -> new IllegalArgumentException("Design not found"));

            design.setActive(true);
            inventoryDesignRepository.save(design);

            log.info("Inventory design activated. User ID: {}, Design ID: {}", user.getId(), id);

        } catch (IllegalArgumentException e) {
            log.warn("Unable to activate inventory design. User ID: {}, Design ID: {}, Message: {}",
                    user != null ? user.getId() : null, id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while activating inventory design. User ID: {}, Design ID: {}",
                    user != null ? user.getId() : null, id, e);
            throw new RuntimeException("Unable to activate inventory design", e);
        }
    }

    public List<InventoryDesign> getAllDesigns(User user) {
        return inventoryDesignRepository.findByUserOrderByNameAsc(user);
    }
}