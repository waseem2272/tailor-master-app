package com.example.tailormaster.service;

import com.example.tailormaster.entity.StockMovement;
import com.example.tailormaster.enums.StockMovementType;
import com.example.tailormaster.repository.StockMovementRepository;
import com.example.tailormaster.util.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public List<StockMovement> getAll() {
        return stockMovementRepository.findByUserOrderByCreatedAtDesc(
                authenticatedUserService.getCurrentUser());
    }

    public List<StockMovement> getByItemId(Long inventoryItemId) {
        return stockMovementRepository.findByUserAndInventoryItemIdOrderByCreatedAtDesc(
                authenticatedUserService.getCurrentUser(), inventoryItemId);
    }

    public List<StockMovement> getByMovementType(StockMovementType movementType) {
        return stockMovementRepository.findByUserAndMovementTypeOrderByCreatedAtDesc(
                authenticatedUserService.getCurrentUser(), movementType);
    }

    public List<StockMovement> getByItemAndMovementType(
            Long inventoryItemId,
            StockMovementType movementType) {

        return stockMovementRepository
                .findByUserAndInventoryItemIdAndMovementTypeOrderByCreatedAtDesc(
                        authenticatedUserService.getCurrentUser(),
                        inventoryItemId,
                        movementType);
    }
}