package com.example.tailormaster.service;

import com.example.tailormaster.entity.InventoryBrand;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.repository.InventoryBrandRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryBrandService {

    private final InventoryBrandRepository inventoryBrandRepository;

    @Transactional
    public List<InventoryBrand> getAllBrands(User user) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User is required");
            }

            return inventoryBrandRepository.findByUserOrderByNameAsc(user);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request while fetching brands. User ID: {}, Message: {}",
                    user != null ? user.getId() : null, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while fetching inventory brands. User ID: {}",
                    user != null ? user.getId() : null, e);
            throw new RuntimeException("Unable to fetch inventory brands", e);
        }
    }

    @Transactional
    public List<InventoryBrand> getActiveBrands(User user) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User is required");
            }

            return inventoryBrandRepository.findByUserAndActiveTrueOrderByNameAsc(user);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request while fetching active brands. User ID: {}, Message: {}",
                    user != null ? user.getId() : null, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while fetching active inventory brands. User ID: {}",
                    user != null ? user.getId() : null, e);
            throw new RuntimeException("Unable to fetch active inventory brands", e);
        }
    }

    @Transactional
    public InventoryBrand getById(User user, Long id) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User is required");
            }
            if (id == null) {
                throw new IllegalArgumentException("Brand ID is required");
            }

            return inventoryBrandRepository.findByUserAndId(user, id)
                    .orElseThrow(() -> new IllegalArgumentException("Brand not found"));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request while fetching brand. User ID: {}, Brand ID: {}, Message: {}",
                    user != null ? user.getId() : null, id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while fetching inventory brand. User ID: {}, Brand ID: {}",
                    user != null ? user.getId() : null, id, e);
            throw new RuntimeException("Unable to fetch inventory brand", e);
        }
    }

    @Transactional
    public InventoryBrand save(User user, InventoryBrand brand) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User is required");
            }
            if (brand == null) {
                throw new IllegalArgumentException("Brand is required");
            }
            if (brand.getName() == null || brand.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Brand name is required");
            }

            String name = brand.getName().trim();

            boolean duplicate = inventoryBrandRepository.findByUserAndNameIgnoreCase(user, name)
                    .filter(existing -> brand.getId() == null || !existing.getId().equals(brand.getId()))
                    .isPresent();

            if (duplicate) {
                throw new IllegalArgumentException("Brand already exists: " + name);
            }

            InventoryBrand brandToSave;

            if (brand.getId() == null) {
                brand.setUser(user);
                brand.setName(name);
                brand.setActive(true);
                brandToSave = brand;
            } else {
                brandToSave = inventoryBrandRepository.findByUserAndId(user, brand.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Brand not found"));

                brandToSave.setName(name);
            }

            InventoryBrand savedBrand = inventoryBrandRepository.save(brandToSave);

            log.info("Inventory brand saved successfully. User ID: {}, Brand ID: {}, Name: {}",
                    user.getId(), savedBrand.getId(), savedBrand.getName());

            return savedBrand;

        } catch (IllegalArgumentException e) {
            log.warn("Invalid inventory brand data. User ID: {}, Message: {}",
                    user != null ? user.getId() : null, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while saving inventory brand. User ID: {}",
                    user != null ? user.getId() : null, e);
            throw new RuntimeException("Unable to save inventory brand", e);
        }
    }

    @Transactional
    public void deactivate(User user, Long id) {
        try {
            InventoryBrand brand = getById(user, id);
            brand.setActive(false);
            inventoryBrandRepository.save(brand);

            log.info("Inventory brand deactivated. User ID: {}, Brand ID: {}, Name: {}",
                    user.getId(), brand.getId(), brand.getName());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request while deactivating brand. User ID: {}, Brand ID: {}, Message: {}",
                    user != null ? user.getId() : null, id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while deactivating inventory brand. User ID: {}, Brand ID: {}",
                    user != null ? user.getId() : null, id, e);
            throw new RuntimeException("Unable to deactivate inventory brand", e);
        }
    }

    @Transactional
    public void activate(User user, Long id) {
        try {
            InventoryBrand brand = getById(user, id);
            brand.setActive(true);
            inventoryBrandRepository.save(brand);

            log.info("Inventory brand activated. User ID: {}, Brand ID: {}, Name: {}",
                    user.getId(), brand.getId(), brand.getName());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request while activating brand. User ID: {}, Brand ID: {}, Message: {}",
                    user != null ? user.getId() : null, id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while activating inventory brand. User ID: {}, Brand ID: {}",
                    user != null ? user.getId() : null, id, e);
            throw new RuntimeException("Unable to activate inventory brand", e);
        }
    }
}