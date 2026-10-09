package com.example.tailormaster.service.backup;

import com.example.tailormaster.entity.BackupHistory;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.entity.UserBackupSettings;
import com.example.tailormaster.enums.BackupStatus;
import com.example.tailormaster.repository.backup.BackupHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    private final UserBackupSettingsService userBackupSettingsService;
    private final BackupHistoryRepository backupHistoryRepository;
    private final DataSource dataSource;
    private final Environment environment;
    private final GoogleDriveService googleDriveService;

    @Override
    public BackupHistory createLocalBackup(User user) {
        BackupHistory history = new BackupHistory();
        history.setUser(user);
        history.setStartedAt(LocalDateTime.now());
        history.setStatus(BackupStatus.STARTED);
        history.setLocalBackupStatus(BackupStatus.STARTED);
        history.setGoogleDriveStatus(BackupStatus.NOT_ATTEMPTED);

        try {
            UserBackupSettings settings = userBackupSettingsService.getByUser(user);

            if (!settings.isLocalBackupEnabled()) {
                history.setStatus(BackupStatus.NOT_ATTEMPTED);
                history.setLocalBackupStatus(BackupStatus.NOT_ATTEMPTED);
                history.setCompletedAt(LocalDateTime.now());
                backupHistoryRepository.save(history);

                log.info("Local backup is disabled for user: {}", user.getUsername());
                return history;
            }

            if (settings.getLocalBackupPath() == null
                    || settings.getLocalBackupPath().isBlank()) {
                throw new IllegalArgumentException(
                        "Local backup location is not configured."
                );
            }

            String jdbcUrl;
            String username;
            String password;

            try (Connection connection = dataSource.getConnection()) {
                jdbcUrl = connection.getMetaData().getURL();
                username = connection.getMetaData().getUserName();

                if (username.contains("@")) {
                    username = username.substring(0, username.indexOf("@"));
                }
            }

            password = environment.getProperty("spring.datasource.password");

            if (password == null) {
                throw new IllegalArgumentException(
                        "Database password is not configured."
                );
            }

            String databaseName = jdbcUrl.substring(jdbcUrl.lastIndexOf("/") + 1);

            int queryIndex = databaseName.indexOf("?");
            if (queryIndex > -1) {
                databaseName = databaseName.substring(0, queryIndex);
            }

            if (databaseName.isBlank()) {
                throw new IllegalArgumentException(
                        "Database name could not be determined from datasource URL."
                );
            }

            Path backupDirectory = Paths.get(settings.getLocalBackupPath());
            Files.createDirectories(backupDirectory);

            String fileName = databaseName + "_"
                    + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            )
                    + ".sql";

            Path backupFile = backupDirectory.resolve(fileName);
            Path errorFile = backupDirectory.resolve(fileName + ".error");

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "C:\\Program Files\\MySQL\\MySQL Workbench 8.0\\mysqldump.exe",
                    "-h", "localhost",
                    "-u", username,
                    "-p" + password,
                    databaseName
            );

            processBuilder.redirectOutput(backupFile.toFile());
            processBuilder.redirectError(errorFile.toFile());

            log.info(
                    "Starting local database backup for user: {}, database: {}",
                    user.getUsername(),
                    databaseName
            );

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String errorMessage = "";

                if (Files.exists(errorFile)) {
                    errorMessage = Files.readString(errorFile).trim();
                }

                Files.deleteIfExists(backupFile);
                Files.deleteIfExists(errorFile);

                throw new RuntimeException(
                        "mysqldump failed with exit code: "
                                + exitCode
                                + (errorMessage.isBlank()
                                ? ""
                                : " - " + errorMessage)
                );
            }

            long fileSize = Files.size(backupFile);

            if (fileSize <= 0) {
                Files.deleteIfExists(backupFile);
                Files.deleteIfExists(errorFile);

                throw new RuntimeException(
                        "mysqldump completed with exit code 0, "
                                + "but the backup file is empty."
                );
            }

            Files.deleteIfExists(errorFile);

            history.setBackupFileName(fileName);
            history.setFileSize(fileSize);
            history.setLocalBackupStatus(BackupStatus.SUCCESS);

            if (settings.isGoogleDriveEnabled()
                    && settings.isGoogleDriveConnected()) {

                try {
                    googleDriveService.uploadBackup(user, backupFile.toFile());

                    history.setGoogleDriveStatus(BackupStatus.SUCCESS);
                    history.setStatus(BackupStatus.SUCCESS);

                    log.info(
                            "Google Drive backup completed successfully for user: {}",
                            user.getUsername()
                    );

                } catch (Exception e) {
                    log.error(
                            "Google Drive backup failed for user: {}",
                            user.getUsername(),
                            e
                    );

                    history.setGoogleDriveStatus(BackupStatus.FAILED);
                    history.setStatus(BackupStatus.PARTIAL_SUCCESS);
                    history.setErrorMessage(
                            "Local backup succeeded, but Google Drive upload failed: "
                                    + e.getMessage()
                    );
                }

            } else {
                history.setGoogleDriveStatus(BackupStatus.NOT_ATTEMPTED);
                history.setStatus(BackupStatus.SUCCESS);
            }

            history.setCompletedAt(LocalDateTime.now());
            backupHistoryRepository.save(history);

            log.info(
                    "Local database backup completed successfully: {} ({} bytes)",
                    backupFile,
                    fileSize
            );

            return history;

        } catch (Exception e) {
            log.error(
                    "Error creating local database backup for user: {}",
                    user.getUsername(),
                    e
            );

            history.setLocalBackupStatus(BackupStatus.FAILED);
            history.setStatus(BackupStatus.FAILED);
            history.setCompletedAt(LocalDateTime.now());
            history.setErrorMessage(e.getMessage());

            backupHistoryRepository.save(history);
            return history;
        }
    }

    @Override
    public void cleanupOldLocalBackups(User user) {
        try {
            UserBackupSettings settings = userBackupSettingsService.getByUser(user);

            if (settings == null || !settings.isLocalBackupEnabled()) {
                return;
            }

            if (settings.getRetentionDays() == null || settings.getRetentionDays() <= 0) {
                log.warn("Invalid backup retention days for user: {}", user.getUsername());
                return;
            }

            if (settings.getLocalBackupPath() == null
                    || settings.getLocalBackupPath().isBlank()) {
                log.warn("Local backup location is not configured for user: {}", user.getUsername());
                return;
            }

            LocalDateTime cutoff = LocalDate.now()
                    .minusDays(settings.getRetentionDays() - 1L)
                    .atStartOfDay();

            log.info("Backup cleanup user: {}, retentionDays: {}, cutoff: {}",
                    user.getUsername(), settings.getRetentionDays(), cutoff);

            List<BackupHistory> oldBackups =
                    backupHistoryRepository
                            .findByUserAndStartedAtBeforeAndBackupFileNameIsNotNull(user, cutoff);

            log.info("Expired backup history records found: {}", oldBackups.size());

            Path backupDirectory = Paths.get(settings.getLocalBackupPath())
                    .toAbsolutePath()
                    .normalize();

            log.info("Configured backup directory: {}", backupDirectory);

            for (BackupHistory backup : oldBackups) {
                try {
                    Path backupFile = backupDirectory.resolve(backup.getBackupFileName())
                            .normalize();

                    log.info("Checking expired backup file: {}, exists: {}",
                            backupFile, Files.exists(backupFile));

                    if (!backupFile.getParent().equals(backupDirectory)) {
                        log.warn("Skipping backup file outside configured directory: {}", backupFile);
                        continue;
                    }

                    if (Files.isRegularFile(backupFile)) {
                        Files.delete(backupFile);
                        log.info("Deleted expired local backup for user: {}, file: {}",
                                user.getUsername(), backupFile);
                    }
                } catch (Exception e) {
                    log.error("Error deleting expired local backup for user: {}, file: {}",
                            user.getUsername(), backup.getBackupFileName(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error cleaning up old local backups for user: {}",
                    user != null ? user.getUsername() : "unknown", e);
        }
    }
}