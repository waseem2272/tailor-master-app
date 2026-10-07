package com.example.tailormaster.entity;

import com.example.tailormaster.enums.BackupStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "backup_history")
public class BackupHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    private BackupStatus status;

    @Enumerated(EnumType.STRING)
    private BackupStatus localBackupStatus;

    @Enumerated(EnumType.STRING)
    private BackupStatus googleDriveStatus;

    private String backupFileName;

    private Long fileSize;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}