package com.example.tailormaster.service.backup;

import com.example.tailormaster.entity.User;
import com.example.tailormaster.entity.UserBackupSettings;

public interface UserBackupSettingsService {

    UserBackupSettings getByUser(User user);

    UserBackupSettings save(UserBackupSettings settings);
}