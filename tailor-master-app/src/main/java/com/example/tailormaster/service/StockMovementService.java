package com.example.tailormaster.service;

import com.example.tailormaster.entity.StockMovement;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.enums.StockMovementType;
import com.example.tailormaster.repository.StockMovementRepository;
import com.example.tailormaster.util.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public Page<StockMovement> getPaginated(
            int page,
            int size,
            Long itemId,
            StockMovementType movementType) {

        User user = authenticatedUserService.getCurrentUser();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        if (itemId != null && movementType != null) {
            return stockMovementRepository
                    .findByUserAndInventoryItemIdAndMovementTypeOrderByCreatedAtDesc(
                            user,
                            itemId,
                            movementType,
                            pageable
                    );
        }

        if (itemId != null) {
            return stockMovementRepository
                    .findByUserAndInventoryItemIdOrderByCreatedAtDesc(
                            user,
                            itemId,
                            pageable
                    );
        }

        if (movementType != null) {
            return stockMovementRepository
                    .findByUserAndMovementTypeOrderByCreatedAtDesc(
                            user,
                            movementType,
                            pageable
                    );
        }

        return stockMovementRepository.findByUserOrderByCreatedAtDesc(
                user,
                pageable
        );
    }
}