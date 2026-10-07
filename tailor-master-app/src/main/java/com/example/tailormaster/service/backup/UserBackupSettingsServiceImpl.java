package com.example.tailormaster.service.backup;

import com.example.tailormaster.entity.User;
import com.example.tailormaster.entity.UserBackupSettings;
import com.example.tailormaster.repository.backup.UserBackupSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserBackupSettingsServiceImpl implements UserBackupSettingsService {

    private final UserBackupSettingsRepository userBackupSettingsRepository;

    @Override
    public UserBackupSettings getByUser(User user) {
        return userBackupSettingsRepository.findByUser(user)
                .orElseGet(() -> {
                    UserBackupSettings settings = new UserBackupSettings();
                    settings.setUser(user);
                    return userBackupSettingsRepository.save(settings);
                });
    }

    @Override
    public UserBackupSettings save(UserBackupSettings settings) {
        return userBackupSettingsRepository.save(settings);
    }
}