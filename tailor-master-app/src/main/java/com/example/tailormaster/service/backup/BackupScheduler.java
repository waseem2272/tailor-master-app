package com.example.tailormaster.service.backup;

import com.example.tailormaster.entity.User;
import com.example.tailormaster.entity.UserBackupSettings;
import com.example.tailormaster.repository.backup.UserBackupSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    private final UserBackupSettingsRepository userBackupSettingsRepository;
    private final BackupService backupService;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void runScheduledBackup() {
        try {
            LocalTime currentTime = LocalTime.now().withSecond(0).withNano(0);

            List<UserBackupSettings> settingsList =
                    userBackupSettingsRepository.findByAutomaticBackupEnabledTrue();

            for (UserBackupSettings settings : settingsList) {

                if (!settings.isLocalBackupEnabled()
                        || settings.getBackupTime() == null
                        || !settings.getBackupTime().equals(currentTime)) {
                    continue;
                }

                User user = settings.getUser();

                log.info("Starting scheduled backup for user: {}", user.getUsername());

                backupService.createLocalBackup(user);
            }

        } catch (Exception e) {
            log.error("Error running scheduled backup", e);
        }
    }
}