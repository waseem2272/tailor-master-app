package com.example.tailormaster.service.backup;

import com.example.tailormaster.entity.BackupHistory;
import com.example.tailormaster.entity.User;

public interface BackupService {
    BackupHistory createLocalBackup(User user);
}