package com.example.tailormaster.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_backup_settings")
public class UserBackupSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private boolean automaticBackupEnabled = false;

    private LocalTime backupTime = LocalTime.of(2, 0);

    private boolean localBackupEnabled = true;

    private String localBackupPath;

    private boolean googleDriveEnabled = false;

    private boolean googleDriveConnected = false;

    private String googleDriveFolderId;

    private Integer retentionDays = 30;
}