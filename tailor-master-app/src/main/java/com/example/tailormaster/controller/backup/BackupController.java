package com.example.tailormaster.controller.backup;

import com.example.tailormaster.entity.BackupHistory;
import com.example.tailormaster.entity.User;
import com.example.tailormaster.entity.UserBackupSettings;
import com.example.tailormaster.enums.BackupStatus;
import com.example.tailormaster.repository.backup.BackupHistoryRepository;
import com.example.tailormaster.service.backup.BackupService;
import com.example.tailormaster.service.backup.GoogleDriveService;
import com.example.tailormaster.service.backup.UserBackupSettingsService;
import com.example.tailormaster.util.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/backup")
@RequiredArgsConstructor
public class BackupController {

    private final UserBackupSettingsService userBackupSettingsService;
    private final AuthenticatedUserService authenticatedUserService;
    private final BackupService backupService;
    private final BackupHistoryRepository backupHistoryRepository;
    private final GoogleDriveService googleDriveService;

    @GetMapping("/settings")
    public String settings(Model model) {
        try {
            User user = authenticatedUserService.getCurrentUser();
            UserBackupSettings settings = userBackupSettingsService.getByUser(user);
            model.addAttribute("settings", settings);
            model.addAttribute("activePage", "backup/settings");
            return "backup/settings";
        } catch (Exception e) {
            log.error("Error loading backup settings", e);
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/settings")
    public String saveSettings(@ModelAttribute("settings") UserBackupSettings settings,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = authenticatedUserService.getCurrentUser();
            UserBackupSettings existingSettings = userBackupSettingsService.getByUser(user);

            existingSettings.setAutomaticBackupEnabled(settings.isAutomaticBackupEnabled());
            existingSettings.setBackupTime(settings.getBackupTime());
            existingSettings.setLocalBackupEnabled(settings.isLocalBackupEnabled());
            existingSettings.setLocalBackupPath(settings.getLocalBackupPath());
            existingSettings.setGoogleDriveEnabled(settings.isGoogleDriveEnabled());
            existingSettings.setRetentionDays(settings.getRetentionDays());

            userBackupSettingsService.save(existingSettings);

            log.info("Backup settings updated successfully for user: {}", user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Backup settings saved successfully.");
        } catch (Exception e) {
            log.error("Error saving backup settings", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to save backup settings.");
        }
        return "redirect:/backup/settings";
    }

    @PostMapping("/now")
    public String backupNow(RedirectAttributes redirectAttributes) {
        try {
            User user = authenticatedUserService.getCurrentUser();
            BackupHistory history = backupService.createLocalBackup(user);

            if (history.getStatus() == BackupStatus.SUCCESS) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Database backup completed successfully."
                );
            } else if (history.getStatus() == BackupStatus.PARTIAL_SUCCESS) {
                redirectAttributes.addFlashAttribute(
                        "warningMessage",
                        "Local backup completed successfully, but Google Drive backup failed."
                );
            } else {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        history.getErrorMessage() != null
                                ? history.getErrorMessage()
                                : "Unable to create database backup."
                );
            }
        } catch (Exception e) {
            log.error("Error starting manual backup", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to create database backup."
            );
        }
        return "redirect:/backup/settings";
    }

    @GetMapping("/history")
    public String history(Model model) {
        try {
            User user = authenticatedUserService.getCurrentUser();
            List<BackupHistory> history = backupHistoryRepository.findByUserOrderByStartedAtDesc(user);
            model.addAttribute("history", history);
            model.addAttribute("activePage", "backup/history");
            return "backup/history";
        } catch (Exception e) {
            log.error("Error loading backup history", e);
            return "redirect:/backup/settings";
        }
    }

    @GetMapping("/google/connect")
    public String connectGoogleDrive() {
        try {
            User user = authenticatedUserService.getCurrentUser();
            String authorizationUrl = googleDriveService.getAuthorizationUrl(user);
            return "redirect:" + authorizationUrl;
        } catch (Exception e) {
            log.error("Error connecting Google Drive", e);
            return "redirect:/backup/settings";
        }
    }

    @GetMapping("/google/callback")
    public String googleCallback(@RequestParam("code") String code,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = authenticatedUserService.getCurrentUser();

            googleDriveService.handleCallback(user, code);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Google Drive connected successfully."
            );
        } catch (Exception e) {
            log.error("Error handling Google Drive callback", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to connect Google Drive."
            );
        }

        return "redirect:/backup/settings";
    }

    @PostMapping("/google/disconnect")
    public String disconnectGoogleDrive(RedirectAttributes redirectAttributes) {
        try {
            User user = authenticatedUserService.getCurrentUser();

            googleDriveService.disconnect(user);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Google Drive disconnected successfully."
            );
        } catch (Exception e) {
            log.error("Error disconnecting Google Drive", e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to disconnect Google Drive."
            );
        }

        return "redirect:/backup/settings";
    }
}