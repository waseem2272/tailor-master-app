package com.example.tailormaster.repository.backup;

import com.example.tailormaster.entity.BackupHistory;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.enums.BackupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BackupHistoryRepository extends JpaRepository<BackupHistory, Long> {

    List<BackupHistory> findByUserOrderByStartedAtDesc(User user);
    List<BackupHistory> findByUserAndStartedAtBeforeAndBackupFileNameIsNotNull(
            User user,
            LocalDateTime cutoff
    );

    boolean existsByUserAndStartedAtGreaterThanEqualAndStatus(
            User user,
            LocalDateTime startOfDay,
            BackupStatus status
    );

}