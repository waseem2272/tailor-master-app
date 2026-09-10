package com.example.tailormaster.service;

import com.example.tailormaster.entity.StockMovement;
import com.example.tailormaster.enums.StockMovementType;
import com.example.tailormaster.repository.StockMovementRepository;
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

    public List<StockMovement> getAll() {
        return stockMovementRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<StockMovement> getByItemId(Long inventoryItemId) {
        return stockMovementRepository.findByInventoryItemIdOrderByCreatedAtDesc(inventoryItemId);
    }

    public List<StockMovement> getByMovementType(StockMovementType movementType) {
        return stockMovementRepository.findByMovementTypeOrderByCreatedAtDesc(movementType);
    }

    public List<StockMovement> getByItemAndMovementType(
            Long inventoryItemId,
            StockMovementType movementType) {

        return stockMovementRepository
                .findByInventoryItemIdAndMovementTypeOrderByCreatedAtDesc(
                        inventoryItemId,
                        movementType
                );
    }
}