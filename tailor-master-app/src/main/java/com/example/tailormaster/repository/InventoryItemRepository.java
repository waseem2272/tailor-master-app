package com.example.tailormaster.repository;

import com.example.tailormaster.entity.InventoryItem;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.enums.ItemType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByUserAndActiveTrue(User user);

    List<InventoryItem> findByUserAndItemTypeAndActiveTrue(User user, ItemType itemType);

    List<InventoryItem> findByUserAndNameContainingIgnoreCaseAndActiveTrue(User user, String name);

    Page<InventoryItem> findByUserAndActiveTrue(
            User user,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.id = :id AND i.user = :user")
    Optional<InventoryItem> findByIdForUpdate(
            @Param("id") Long id,
            @Param("user") User user
    );

    Optional<InventoryItem> findByUserAndId(User user, Long id);

    @Query("""
            SELECT i
            FROM InventoryItem i
            LEFT JOIN i.brand b
            LEFT JOIN i.category cat
            LEFT JOIN i.color col
            WHERE i.user = :user
              AND i.active = true
                AND (
                    :name IS NULL
                    OR LOWER(i.name) LIKE LOWER(CONCAT('%', :name, '%'))
                    OR LOWER(b.name) LIKE LOWER(CONCAT('%', :name, '%'))
                    OR LOWER(cat.name) LIKE LOWER(CONCAT('%', :name, '%'))
                    OR LOWER(col.name) LIKE LOWER(CONCAT('%', :name, '%'))
                    OR LOWER(CAST(i.unit AS string)) LIKE LOWER(CONCAT('%', :name, '%'))
                )
              AND (:itemType IS NULL OR i.itemType = :itemType)
              AND (
                    :stockStatus IS NULL
                    OR :stockStatus = ''
                    OR (
                        :stockStatus = 'out'
                        AND i.quantity = 0
                    )
                    OR (
                         :stockStatus = 'available'
                         AND (
                             i.quantity - COALESCE(
                                 (
                                     SELECT SUM(r.quantity)
                                     FROM OrderInventoryReservation r
                                     WHERE r.user = :user
                                       AND r.inventoryItem.id = i.id
                                       AND r.released = false
                                 ),
                                 0
                             )
                         ) > 0
                         AND (
                             i.minimumStock IS NULL
                             OR (
                                 i.quantity - COALESCE(
                                     (
                                         SELECT SUM(r.quantity)
                                         FROM OrderInventoryReservation r
                                         WHERE r.user = :user
                                           AND r.inventoryItem.id = i.id
                                           AND r.released = false
                                     ),
                                     0
                                 )
                             ) > i.minimumStock
                         )
                     )
                    OR (
                        :stockStatus = 'low'
                        AND (
                            i.quantity - COALESCE(
                                (
                                    SELECT SUM(r.quantity)
                                    FROM OrderInventoryReservation r
                                    WHERE r.user = :user
                                      AND r.inventoryItem.id = i.id
                                      AND r.released = false
                                ),
                                0
                            )
                        ) > 0
                        AND (
                            i.quantity - COALESCE(
                                (
                                    SELECT SUM(r.quantity)
                                    FROM OrderInventoryReservation r
                                    WHERE r.user = :user
                                      AND r.inventoryItem.id = i.id
                                      AND r.released = false
                                ),
                                0
                            )
                        ) <= i.minimumStock
                    )
              )
            """)
    Page<InventoryItem> findInventoryWithFilters(
            @Param("user") User user,
            @Param("name") String name,
            @Param("itemType") ItemType itemType,
            @Param("stockStatus") String stockStatus,
            Pageable pageable
    );

}