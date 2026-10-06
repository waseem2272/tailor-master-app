package com.example.tailormaster.service;

import com.example.tailormaster.entity.InventoryCategory;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.enums.ItemType;
import com.example.tailormaster.repository.InventoryCategoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryCategoryService {

    private final InventoryCategoryRepository inventoryCategoryRepository;

    @Transactional
    public List<InventoryCategory> getAllCategories(User user) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User is required");
            }

            return inventoryCategoryRepository.findByUserOrderByCreatedAtDesc(user);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request while fetching categories. User ID: {}, Message: {}",
                    user != null ? user.getId() : null, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while fetching inventory categories. User ID: {}",
                    user != null ? user.getId() : null, e);
            throw new RuntimeException("Unable to fetch inventory categories", e);
        }
    }

    @Transactional
    public List<InventoryCategory> getActiveCategories(User user) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User is required");
            }

            return inventoryCategoryRepository.findByUserAndActiveTrueOrderByNameAsc(user);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request while fetching active categories. User ID: {}, Message: {}",
                    user != null ? user.getId() : null, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while fetching active inventory categories. User ID: {}",
                    user != null ? user.getId() : null, e);
            throw new RuntimeException("Unable to fetch active inventory categories", e);
        }
    }

    @Transactional
    public List<InventoryCategory> getActiveCategoriesByItemType(User user, ItemType itemType) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User is required");
            }
            if (itemType == null) {
                throw new IllegalArgumentException("Item type is required");
            }

            return inventoryCategoryRepository
                    .findByUserAndItemTypeAndActiveTrueOrderByNameAsc(user, itemType);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request while fetching categories by item type. User ID: {}, Item Type: {}, Message: {}",
                    user != null ? user.getId() : null, itemType, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while fetching categories by item type. User ID: {}, Item Type: {}",
                    user != null ? user.getId() : null, itemType, e);
            throw new RuntimeException("Unable to fetch categories by item type", e);
        }
    }

    @Transactional
    public InventoryCategory getById(User user, Long id) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User is required");
            }
            if (id == null) {
                throw new IllegalArgumentException("Category ID is required");
            }

            return inventoryCategoryRepository.findByUserAndId(user, id)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request while fetching category. User ID: {}, Category ID: {}, Message: {}",
                    user != null ? user.getId() : null, id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while fetching inventory category. User ID: {}, Category ID: {}",
                    user != null ? user.getId() : null, id, e);
            throw new RuntimeException("Unable to fetch inventory category", e);
        }
    }

    @Transactional
    public InventoryCategory save(User user, InventoryCategory category) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User is required");
            }
            if (category == null) {
                throw new IllegalArgumentException("Category is required");
            }
            if (category.getName() == null || category.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Category name is required");
            }
            if (category.getItemType() == null) {
                throw new IllegalArgumentException("Item type is required");
            }

            String name = category.getName().trim();
            ItemType itemType = category.getItemType();

            boolean duplicate = inventoryCategoryRepository
                    .existsByUserAndNameIgnoreCaseAndItemType(user, name, itemType);

            if (duplicate) {
                if (category.getId() == null) {
                    throw new IllegalArgumentException(
                            "Category already exists for item type: " + name + " (" + itemType + ")");
                }

                InventoryCategory existing = inventoryCategoryRepository
                        .findByUserAndId(user, category.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Category not found"));

                if (!existing.getName().equalsIgnoreCase(name)
                        || existing.getItemType() != itemType) {
                    throw new IllegalArgumentException(
                            "Category already exists for item type: " + name + " (" + itemType + ")");
                }
            }

            InventoryCategory categoryToSave;

            if (category.getId() == null) {
                category.setUser(user);
                category.setName(name);
                category.setActive(true);
                categoryToSave = category;
            } else {
                categoryToSave = inventoryCategoryRepository
                        .findByUserAndId(user, category.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Category not found"));

                categoryToSave.setName(name);
                categoryToSave.setItemType(itemType);
            }

            InventoryCategory savedCategory = inventoryCategoryRepository.save(categoryToSave);

            log.info("Inventory category saved successfully. User ID: {}, Category ID: {}, Name: {}, Item Type: {}",
                    user.getId(), savedCategory.getId(), savedCategory.getName(), savedCategory.getItemType());

            return savedCategory;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid inventory category data. User ID: {}, Message: {}",
                    user != null ? user.getId() : null, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while saving inventory category. User ID: {}",
                    user != null ? user.getId() : null, e);
            throw new RuntimeException("Unable to save inventory category", e);
        }
    }

    @Transactional
    public void deactivate(User user, Long id) {
        try {
            InventoryCategory category = getById(user, id);
            category.setActive(false);
            inventoryCategoryRepository.save(category);

            log.info("Inventory category deactivated. User ID: {}, Category ID: {}, Name: {}",
                    user.getId(), category.getId(), category.getName());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request while deactivating category. User ID: {}, Category ID: {}, Message: {}",
                    user != null ? user.getId() : null, id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while deactivating inventory category. User ID: {}, Category ID: {}",
                    user != null ? user.getId() : null, id, e);
            throw new RuntimeException("Unable to deactivate inventory category", e);
        }
    }

    @Transactional
    public void activate(User user, Long id) {
        try {
            InventoryCategory category = getById(user, id);
            category.setActive(true);
            inventoryCategoryRepository.save(category);

            log.info("Inventory category activated. User ID: {}, Category ID: {}, Name: {}",
                    user.getId(), category.getId(), category.getName());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request while activating category. User ID: {}, Category ID: {}, Message: {}",
                    user != null ? user.getId() : null, id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error while activating inventory category. User ID: {}, Category ID: {}",
                    user != null ? user.getId() : null, id, e);
            throw new RuntimeException("Unable to activate inventory category", e);
        }
    }
}