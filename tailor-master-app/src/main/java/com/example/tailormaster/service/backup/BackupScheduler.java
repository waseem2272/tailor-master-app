package com.example.tailormaster.service.backup;

import com.example.tailormaster.entity.BackupHistory;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.entity.UserBackupSettings;
import com.example.tailormaster.enums.BackupStatus;
import com.example.tailormaster.repository.backup.BackupHistoryRepository;
import com.example.tailormaster.repository.backup.UserBackupSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    private final UserBackupSettingsRepository userBackupSettingsRepository;
    private final BackupHistoryRepository backupHistoryRepository;
    private final BackupService backupService;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void runScheduledBackup() {
        checkScheduledBackups(false);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void runMissedBackupOnStartup() {
        log.info("Application ready event triggered: checking missed backups");
        checkScheduledBackups(true);
    }

    private void checkScheduledBackups(boolean startupCheck) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalTime currentTime = now.toLocalTime().withSecond(0).withNano(0);

            List<UserBackupSettings> settingsList =
                    userBackupSettingsRepository.findByAutomaticBackupEnabledTrue();

            for (UserBackupSettings settings : settingsList) {
                if (!settings.isLocalBackupEnabled() || settings.getBackupTime() == null) {
                    continue;
                }

                if (!startupCheck && !settings.getBackupTime().equals(currentTime)) {
                    continue;
                }

                User user = settings.getUser();

                LocalDate scheduledDate = now.toLocalDate();

                if (startupCheck && currentTime.isBefore(settings.getBackupTime())) {
                    scheduledDate = scheduledDate.minusDays(1);
                }

                LocalDateTime scheduledDateTime =
                        scheduledDate.atTime(settings.getBackupTime());

                boolean backupAlreadySuccessful =
                        backupHistoryRepository
                                .existsByUserAndStartedAtGreaterThanEqualAndStatus(
                                        user, scheduledDateTime, BackupStatus.SUCCESS);

                if (backupAlreadySuccessful) {
                    log.info("Backup already completed for user: {}",
                            user.getUsername());
                    continue;
                }

                log.info("Starting {} backup for user: {}",
                        startupCheck ? "missed scheduled" : "scheduled",
                        user.getUsername());

                BackupHistory history = backupService.createLocalBackup(user);

                log.info("Backup processing finished for user: {}, status: {}",
                        user.getUsername(), history.getStatus());
            }
        } catch (Exception e) {
            log.error("Error checking scheduled backups", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldBackups() {
        try {
            List<UserBackupSettings> settingsList =
                    userBackupSettingsRepository.findAll();

            for (UserBackupSettings settings : settingsList) {
                if (!settings.isLocalBackupEnabled()) {
                    continue;
                }

                User user = settings.getUser();

                log.info("Starting local backup cleanup for user: {}",
                        user.getUsername());

                backupService.cleanupOldLocalBackups(user);
            }
        } catch (Exception e) {
            log.error("Error running scheduled backup cleanup", e);
        }
    }
}