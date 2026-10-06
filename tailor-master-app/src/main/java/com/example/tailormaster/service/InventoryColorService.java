package com.example.tailormaster.service;

import com.example.tailormaster.entity.InventoryColor;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.repository.InventoryColorRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryColorService {

    private final InventoryColorRepository inventoryColorRepository;

    public List<InventoryColor> getActiveColors(User user) {
        return inventoryColorRepository.findByUserAndActiveTrueOrderByNameAsc(user);
    }

    public List<InventoryColor> getAllColors(User user) {
        return inventoryColorRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Optional<InventoryColor> findById(User user, Long id) {
        return inventoryColorRepository.findByUserAndId(user, id);
    }

    @Transactional
    public InventoryColor save(User user, InventoryColor color) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User is required");
            }
            if (color == null) {
                throw new IllegalArgumentException("Color is required");
            }
            if (color.getName() == null || color.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Color name is required");
            }

            String name = color.getName().trim();

            boolean duplicate = inventoryColorRepository.findByUserAndNameIgnoreCase(user, name)
                    .filter(existing -> color.getId() == null || !existing.getId().equals(color.getId()))
                    .isPresent();

            if (duplicate) {
                throw new IllegalArgumentException("Color already exists: " + name);
            }

            InventoryColor colorToSave;

            if (color.getId() == null) {
                color.setUser(user);
                color.setName(name);
                color.setActive(true);
                colorToSave = color;
            } else {
                colorToSave = inventoryColorRepository.findByUserAndId(user, color.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Color not found"));

                colorToSave.setName(name);
                colorToSave.setHexCode(color.getHexCode());
            }

            InventoryColor savedColor = inventoryColorRepository.save(colorToSave);

            log.info("Inventory color saved successfully. User ID: {}, Color ID: {}, Name: {}",
                    user.getId(), savedColor.getId(), savedColor.getName());

            return savedColor;

        } catch (IllegalArgumentException e) {
            log.warn("Invalid inventory color data. User ID: {}, Message: {}",
                    user != null ? user.getId() : null, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while saving inventory color. User ID: {}",
                    user != null ? user.getId() : null, e);
            throw new RuntimeException("Unable to save inventory color", e);
        }
    }

    @Transactional
    public void deactivate(User user, Long id) {
        try {
            InventoryColor color = inventoryColorRepository.findByUserAndId(user, id)
                    .orElseThrow(() -> new IllegalArgumentException("Color not found"));

            color.setActive(false);
            inventoryColorRepository.save(color);

            log.info("Inventory color deactivated. User ID: {}, Color ID: {}", user.getId(), id);

        } catch (IllegalArgumentException e) {
            log.warn("Unable to deactivate inventory color. User ID: {}, Color ID: {}, Message: {}",
                    user != null ? user.getId() : null, id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while deactivating inventory color. User ID: {}, Color ID: {}",
                    user != null ? user.getId() : null, id, e);
            throw new RuntimeException("Unable to deactivate inventory color", e);
        }
    }

    @Transactional
    public void activate(User user, Long id) {
        try {
            InventoryColor color = inventoryColorRepository.findByUserAndId(user, id)
                    .orElseThrow(() -> new IllegalArgumentException("Color not found"));

            color.setActive(true);
            inventoryColorRepository.save(color);

            log.info("Inventory color activated. User ID: {}, Color ID: {}", user.getId(), id);

        } catch (IllegalArgumentException e) {
            log.warn("Unable to activate inventory color. User ID: {}, Color ID: {}, Message: {}",
                    user != null ? user.getId() : null, id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while activating inventory color. User ID: {}, Color ID: {}",
                    user != null ? user.getId() : null, id, e);
            throw new RuntimeException("Unable to activate inventory color", e);
        }
    }
}